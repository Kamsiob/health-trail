# The Health Trail export container, format version 3

**This file is the specification.** It exists so that somebody holding an encrypted archive and the passphrase can decrypt it in ten years with no access to this app, this repository's owner, or any server. `contract/DATA-CONTRACT.md` section 8.1 names it as one of the three requirements that make the archive outlive this project, alongside the standalone tool at `tools/decrypt/` and the passphrase measures in section 6.

**It is written to be sufficient.** `tools/decrypt/decrypt.py` was written from this document rather than from the app's source, which is the test of whether a specification is a specification: if it is enough to build the tool, it is enough for somebody else to build one, years from now, in whatever language exists then.

**Binding on every platform.** A file written by the Android app imports into the web version and the reverse, with no conversion step. The governing rule, from the universal data portability standard: **anything the app can store, the export contains and the import restores.** A feature that stores something the export does not carry silently loses records on device migration, which for this audience is the worst failure the app can have.

**Where this file and `contract/DATA-CONTRACT.md` section 8 disagree, section 8 wins**, and this file is wrong and gets fixed.

---

## 1. The file

```
healthtrail-export-YYYYMMDD-HHMM.zip
```

An ordinary **ZIP64** archive. Not a private format wearing a zip extension: an actual zip, named `.zip`, so that a person who copies it to a laptop and double clicks it gets a folder rather than nothing. That is the first step of the only procedure that matters here, and a private extension breaks it. D98. Versions before 3 used `.htx`.

**ZIP64 rather than classic zip.** The classic central directory keeps a sixteen bit entry count and thirty two bit offsets, so an archive crossing either limit is silently wrong rather than loudly broken. A five year notebook with a photograph of every letter crosses the entry count long before it crosses four gigabytes.

The outer archive holds **exactly three entries, and nothing else**:

```
MANIFEST.json    the header, in the clear
README.txt       how to open this without this app, in the clear
payload.enc      everything else
```

**Nothing in the outer layer says anything about the person or their record.** Not their name, not how many entries there are, not what kind of care it is, not the locale. A backup agent, a cloud sync, or a file manager preview learns that this is a Health Trail archive and when it was made, and nothing more.

Order is not significant to a reader, but writers put `MANIFEST.json` first so a tool reading a very large archive as a stream learns the format version and the key derivation parameters without seeking.

---

## 2. MANIFEST.json

UTF-8, JSON, no BOM.

```json
{
  "format_version": 3,
  "app_version": "0.1.0",
  "platform": "android",
  "schema_version": 3,
  "exported_at": 1785824613306,
  "encrypted": true,
  "passphrase_hint": "the blue notebook on the shelf",
  "encryption": {
    "algorithm": "AES-256-GCM",
    "kdf": "Argon2id",
    "kdf_iterations": 3,
    "kdf_memory_kib": 65536,
    "kdf_parallelism": 1,
    "salt": "vzLbd1kufCEaLpdM9x9UAA==",
    "nonce_prefix": "BWghjQ==",
    "chunk_bytes": 1048576
  }
}
```

| Field | Type | Meaning |
|---|---|---|
| `format_version` | integer | This document describes 3. A reader that does not know a version **refuses it and says so**, rather than guessing. |
| `app_version` | string | What wrote the file. Informational. |
| `platform` | string | `android`, `web`, `fixture`. Informational. |
| `schema_version` | integer | The database schema the payload was written against. |
| `exported_at` | integer | UTC milliseconds. **A fact about the file, not about the person**, which is why it may sit in the clear. Section 8. |
| `encrypted` | boolean | Always `true` in version 3. A reader must **refuse** a file that says `false`, whatever version claims to have written it. |
| `passphrase_hint` | string, optional | What the person wrote to remind themselves. **Absent unless they wrote one.** In the clear by necessity: a hint inside the encryption would need the passphrase to read. |
| `encryption` | object | Present whenever `encrypted`. Every field below is required. |

**`encryption`, field by field.**

| Field | Type | Meaning |
|---|---|---|
| `algorithm` | string | `AES-256-GCM`. 256 bit key, 96 bit nonce, 128 bit tag. |
| `kdf` | string | `Argon2id`. |
| `kdf_iterations` | integer | Argon2id time cost. |
| `kdf_memory_kib` | integer | Argon2id memory cost, in KiB. |
| `kdf_parallelism` | integer | Argon2id lanes. |
| `salt` | string | Base64, standard alphabet with padding. 16 bytes from a CSPRNG, fresh per file. |
| `nonce_prefix` | string | Base64. **4 bytes** from a CSPRNG, fresh per file. Section 4. |
| `chunk_bytes` | integer | How much plaintext one frame of `payload.enc` holds. 1048576 as written today. |

---

## 3. README.txt

US-ASCII, no markup, LF line endings. It is regenerated from the manifest on every export, so it never disagrees with the file it is in.

It states: what the file is; that the contents are encrypted; the passphrase hint, if there is one; that a lost passphrase means the archive cannot be opened by anyone, with no server, no recovery code and no backdoor; the algorithm and every parameter; the frame layout of `payload.enc`; where this specification lives; where `tools/decrypt/` lives; what the inner layout is; and that these are one person's own notes rather than a clinical record.

**ASCII only** so it opens correctly in whatever reads a text file in ten years, on a machine whose defaults nobody can predict.

---

## 4. payload.enc

**The key.** Argon2id over the passphrase as UTF-8 bytes and the manifest's `salt`, at the manifest's three costs, producing 32 bytes.

**The payload is not one AES-GCM message.** One call needs the whole archive in memory at both ends, and this container has to work past four gigabytes, which a phone cannot hold. It is a run of **frames**, laid end to end with nothing before the first and nothing after the last:

```
+---------------+-------------------------------+
| length        | ciphertext                    |
| 4 bytes, BE   | `length` bytes, tag included  |
+---------------+-------------------------------+
```

- **`length`** is an unsigned 32 bit big-endian integer: how many bytes of ciphertext follow, **including** the 16 byte GCM tag.
- Frames are numbered from **0**.
- Every frame but the last holds exactly `chunk_bytes` of plaintext. The last holds whatever remains, which may be `chunk_bytes` and may be zero.
- A zero length payload is written as **one** frame holding zero bytes of plaintext.

**The nonce for frame N** is 12 bytes: the manifest's 4 byte `nonce_prefix`, then N as an unsigned 64 bit big-endian integer.

```
nonce(N) = nonce_prefix || uint64_be(N)
```

**A counter rather than a random nonce per frame.** Random 96 bit nonces collide at a rate that is fine for a handful of messages and not fine for the millions of frames a large archive would have, and a nonce collision under one key breaks GCM outright rather than merely weakening it. The prefix is what keeps two different files out of each other's nonce space.

**The additional authenticated data for frame N** is 9 bytes: N as an unsigned 64 bit big-endian integer, then one byte that is `0x01` on the last frame and `0x00` on every other.

```
aad(N, last) = uint64_be(N) || (last ? 0x01 : 0x00)
```

A reader does not know in advance which frame is last, so it tries the ordinary form first and the final form second; a frame that verifies under neither is a wrong passphrase or an altered file. A reader **must** refuse a payload with no final frame, and **must** refuse anything following the frame that verified as final.

**On what that flag actually earns**, stated plainly because the code's own comments claimed more than was true until they were probed. Two other properties already resist the attacks it is aimed at: a frame moved to another position decrypts under the wrong nonce, and a payload with its tail cut off leaves an inner zip whose central directory is gone. It is kept because it costs nothing and because that second protection is an accident of what is inside the payload today. **A reimplementation must still write and check it**, so that an archive written by one tool opens in another.

**What comes out of the frames, concatenated in order, is an ordinary zip file.** Not a further container, not a custom format. That is the point rather than an implementation detail: somebody with the passphrase and this document gets a folder they can read with tools that already exist.

### 4.1 What implements the cryptography, and what may not

**The algorithms above are the specification, not a preference.** Argon2id for the key derivation and AES-256-GCM for the payload.

**This is not the same as the at-rest database encryption and must never be conflated with it.** The database on the device is encrypted with a key held in the Android Keystore that never leaves the device. A portable file cannot depend on one device's keystore, so it uses a passphrase instead.

**AES-256-GCM comes from the platform.** It is in the Java Cryptography Extension on every supported Android version and needs no dependency.

**Argon2id comes from Bouncy Castle**, `Argon2BytesGenerator`. It is pure Java, so it adds no native library and no NDK build step. Neither the platform nor SQLCipher exposes an Argon2 implementation, which is the whole reason a dependency is needed at all.

**PBKDF2 is not an acceptable substitute.** It is what is already in the platform, which makes it the easy path, and it is a materially weaker claim than this format makes. PBKDF2 is cheap to attack with parallel hardware because it needs almost no memory; Argon2id is memory-hard specifically to remove that advantage. Per D24 **the export file is the only recovery path from key loss**, which makes it the most security sensitive artifact this project produces. Weakening its key derivation to avoid one pure-Java dependency is the wrong trade, and it would be an invisible one: a file encrypted with PBKDF2 looks exactly as safe as one encrypted properly.

### 4.2 Why the parameters travel in the manifest

`kdf_iterations`, `kdf_memory_kib`, and `kdf_parallelism` are recorded in the manifest of every file, and **a reader uses them from the file rather than assuming the values its own build happens to use.**

That is what allows the cost to be raised later without stranding anything. Hardware gets faster and the recommended cost goes up with it; a file written in 2026 must still open in 2036 against whatever the parameters were when it was written. A reader that assumes today's constants silently fails to derive the right key from a **correct** passphrase and reports it as a wrong passphrase, which tells somebody their memory is wrong when their file is fine. That is the worst available failure for somebody's only copy.

**It is enforced rather than trusted.** `tools/checks/check_decrypt_tool.py` writes an archive at a deliberately different Argon2id cost on every run, so a tool with the numbers hard coded fails. That check exists in that shape because the first version of it did not do this and went green against a tool with the memory cost pinned: the hard coded number **is** today's number.

The shipped values, 3 iterations over 64 MiB with parallelism 1, sit above the OWASP baseline rather than at it. **Tune only if it measures unusably slow on the phone**, and if it is tuned, the manifest records what was used and older files keep opening.

**Measured on 2026-08-01: roughly one to one and a half seconds on a Pixel 10 Pro XL**, across repeated runs. Real numbers from `ExportCryptoTest` on the device rather than an estimate, and stated as a range because that is what it does rather than a single figure that would read as more precise than it is. It is entirely bearable for something a person does occasionally and deliberately, so **the cost stays where it is.** The test carries a ten second ceiling so a future device or a raised cost cannot quietly make exporting feel broken.

---

## 5. The inner container

An ordinary zip. Deflate or store, and a reader must handle both.

```
README.txt          the same text as the outer README
MANIFEST.json       the manifest above, plus everything section 8 keeps out of the outer copy
CHECKSUMS.txt       SHA-256 of every other file, one per line
data/trail.sqlite   the whole database, tombstones included
data/schema.sql     the schema it was written against, as commented DDL
readable/           the record as ordinary web pages, per DATA-CONTRACT.md 8.2
attachments/        one file per attachment, named by its SHA-256 in lowercase hex
```

**Entry names are constrained**, so the archive survives being extracted somewhere that is not this phone. US-ASCII only; no path longer than 180 characters; none of `< > : " | ? *` or backslash; no entry whose stem is a reserved Windows device name (`CON`, `PRN`, `AUX`, `NUL`, `COM1` to `COM9`, `LPT1` to `LPT9`); and no two entries differing only by case. A writer that cannot satisfy these **fails the export** rather than writing a file that restores partially somewhere else.

**`README.txt` is byte-identical to the outer one.** Two files with the same name and different words teach a reader to trust neither. The inner copy exists so a folder somebody extracted years ago, long separated from the zip it came out of, still says what it is.

**`MANIFEST.json` here is the full manifest**: everything in section 2 plus `origin_device`, `exported_zone`, `database` (its `sha256`, `byte_size`, `schema_version` and `row_counts`), `attachments` (`count`, `total_bytes`, and `missing` when there are any), `subject_count`, and `readable` (`pages` and `locale`). The outer manifest is this object with the describing half removed, not a different object.

**Three of those were added on 2026-08-10 and the correction is worth stating**, because this document had them wrong rather than merely incomplete. `DATA-CONTRACT.md` 8.2 always listed the export's timezone, the locale the readable copy was written in, and the list of any attachment whose bytes could not be read. This document listed `readable` as carrying `pages` and nothing else, so the code followed this document and wrote none of them. **The data contract governs a data question**, per the precedence in `CLAUDE.md`, so this document is corrected rather than the contract. Issues #210 and #332.

**`exported_zone` is the IANA zone id the exporting device was in**, beside `exported_at`. An epoch alone cannot say what time it was where somebody was standing, which is the same reason every event date in the schema carries a zone next to its instant.

**`readable.locale` is the BCP 47 tag the readable pages are written in**, taken from the words that rendered them rather than from the device, so the manifest cannot disagree with the folder beside it. **A reader needs it to regenerate those pages**: since the pages are written in the person's language, the same rows regenerate correct pages in a different language on a phone set to one, and those are not this archive's bytes. Without this field 8.5's byte identical regeneration is a claim about one phone.

**`attachments.missing` lists the attachments this archive names and does not carry.** Absent when there are none, which is every sound archive; absent and empty mean the same thing. Each entry is an object with `sha256`, `created_at`, and `original_filename` where the record has one:

```json
"attachments": {
  "count": 41,
  "total_bytes": 90371233,
  "missing": [
    { "sha256": "9d98...64ce", "original_filename": "discharge summary.jpg", "created_at": 1785000000000 }
  ]
}
```

**A reader must not refuse an archive over an attachment this list declares.** The row travels, its name and its date travel, and the bytes are gone: `DATA-CONTRACT.md` 8.3 requires it to import as a known-missing attachment "with its name and date intact, so the person sees that a photo existed and is gone, rather than never learning it was there". **An attachment that is absent and not declared is still a failure**, and that difference is the whole reason this is a list rather than a flag: it separates an archive that knows what it is missing from one that was damaged in transit.

`row_counts` carries every table including the ones with zero rows. It exists so an import can state plainly what is about to be imported before doing it, and so a partial file is detectable before any work starts.

**`database.sha256` is the SHA-256 of `data/trail.sqlite` as it sits inside the payload**, in lowercase hex. Plaintext, since it is inside the encryption anyway, so it describes the thing somebody actually wants checked.

**`CHECKSUMS.txt`** is US-ASCII, LF line endings, one line per file, sorted by path:

```
<64 lowercase hex characters><two spaces><path>
```

It lists every entry except itself. **It is not a security measure and does not pretend to be**: it sits inside the encryption, so anything that could forge it could forge the files it describes. It is there so somebody who copied this archive across four machines over ten years can tell which file went bad, in the format every operating system's checksum tool already speaks.

**`data/trail.sqlite` is a plain, unencrypted SQLite database.** It begins with the sixteen bytes `SQLite format 3\0`. It is never the app's at-rest database copied verbatim: that file is SQLCipher, keyed by bytes wrapped in one phone's hardware keystore, and an archive carrying it could only ever be opened by the phone that wrote it, which is a copy rather than a backup. A reader that decrypts the payload successfully and finds something that is not a SQLite file should say so in those terms rather than reporting damage.

**SQLite is the payload** because every target platform reads it natively or through a well maintained WebAssembly build, and because it preserves relationships, ordering and state exactly.

**`data/schema.sql`** is `contract/schema.sql` shipped whole, comments included. A SQLite file tells you its columns and nothing about what they mean.

**`attachments/`** holds one file per attachment, named by the **lowercase hex SHA-256 of its own bytes**, with no extension and no directory nesting. The database row carries the original filename, the mime type and the size. The name is therefore a claim about the contents that a reader can check, and must: a name that does not match its bytes is corruption by definition.

### 5.1 What the database carries that a reader has to handle specially

**Tombstones are included.** An export that drops them cannot restore a deletion, which means restoring a backup resurrects everything the person deleted.

**The change log travels**, and the importer renumbers `seq` locally while preserving `table_name`, `row_id`, `op`, `rev`, `changed_at` and `device_id`. `seq` is meaningful only on the device that wrote it, and two devices will both have a sequence 1 that are not the same event. D12.

**Event dates travel as their EDTF string and are re-derived on the other side.** The `<name>_edtf` column round-trips byte for byte and is what the round trip test asserts equality on. The `<name>_start` and `<name>_end` columns are recomputed from it on import rather than trusted from the file, which is what keeps them an index rather than a second source of truth that can arrive already disagreeing. The zone travels as written, so a date recorded in one zone still reads as the reading the person saw. `contract/DATA-CONTRACT.md` section 3.1.

---

## 6. The passphrase

**Every export is encrypted. There is no unencrypted export path, no chip offering one, and no settings toggle producing one.** D67, confirmed by the owner and recorded as D84. In version 3 an unencrypted archive cannot even be expressed: `payload.enc` is the only place data can go.

**Version 1 offered one, and that was right for what version 1 wrote.** The reasoning was that it is the person's data and wanting to read it is reasonable, which is still true. What changed is what a plain file **is**. Version 1's payload was the device keyed SQLCipher database copied as it sat on disk, so an unencrypted container still held bytes no other machine could read. That was also the defect fixed on 2026-08-02: it made the export unopenable anywhere, which meant the only recovery path from key loss did not exist. **The property that fixed the recovery path is the one that makes a plain file dangerous.**

**Somebody who wants to read their own data still can.** They have the passphrase they chose, the payload is documented SQLite, `schema.sql` travels inside the archive, and `readable/` needs no software at all. What is gone is the file that needs no passphrase.

**At export the person confirms the passphrase twice**, is told plainly that it is the only key and belongs somewhere that is not the phone, and may optionally write a hint. **The app states that anyone who has the file can read the hint**, and the field teaches what belongs there: where the passphrase is written down, not the passphrase.

**A wrong passphrase and an altered file are the same error.** GCM authenticates as well as encrypts, so both mean the tag did not verify, and no reader can tell them apart. A reader **must not** guess which. Telling somebody their file is corrupt when they mistyped is as bad as the reverse, and both sentences are wrong half the time.

---

## 7. Reading, in order

1. Open the outer archive. Read `MANIFEST.json`.
2. If `format_version` is unknown, **stop** and say which version the file is and which are understood. Change nothing.
3. If `encrypted` is not `true`, **stop** and say what the file is: a readable copy of somebody's whole record.
4. Ask for the passphrase. Derive the key with Argon2id using **the file's** salt and costs.
5. Read `payload.enc` frame by frame. Refuse a length absurd for the declared `chunk_bytes` before allocating for it: a length prefix is an instruction from whoever wrote the file, and one that says four gigabytes is how a reader is made to exhaust memory before it has authenticated anything.
6. What comes out is a zip. Read it.
7. Check every file against `CHECKSUMS.txt`, `data/trail.sqlite` against `database.sha256`, and every attachment against its own name.
8. Only then apply anything.

**Nothing is applied until all of it has been read and checked.** An import is atomic: it fully succeeds or it changes nothing. A partially restored state that looks complete is worse than a clean failure, because the person stops worrying.

**Honest about what it cannot do.** If the file contains a table or column this version does not recognize, the import says so and names it rather than silently dropping it.

**Merge or replace is an explicit choice**, described in plain terms, defaulting to whichever is safer for the situation the app can detect. Importing into an empty notebook defaults to replace; importing into one that already has records defaults to merge, because replace would discard what is already there. Merge reconciles per row using the same rules as sync: last write wins on `updated_at`, `origin_device` as a deterministic tiebreaker, tombstones beating earlier edits, and the losing version written to `conflict_log` intact with the person told plainly that two versions existed.

---

## 8. What may sit in the clear, and why the line is where it is

**The export timestamp stays in the outer manifest, and the filename stays human-recognizable.** Decided under `CLAUDE.md` rule 23 and recorded as D86. Removing the date would protect nothing, because the filename already carries it, while costing the person the ability to tell six backups apart.

**An export timestamp is a fact about the file. A date of care is a fact about the person.** That distinction is what the outer layer is drawn around.

**Row counts are not a header, they are a profile.** "1,630 entries, 23 appointments, 9 chapters, six years" describes how ill somebody has been and for how long, to anything that can read the file without the passphrase. They live inside the payload.

**The passphrase hint is the one exception, and it is the person's own.** It sits in the clear because a hint that needs the passphrase is not a hint. The app says so before they write it.

---

## 9. Files that must fail cleanly

Each of these changes nothing, names what was wrong rather than reporting a generic failure, and says that nothing was changed.

| The file | What the reader says |
|---|---|
| Not a zip, or truncated | It could not be opened as a Health Trail export; it may be truncated or a different kind of file. |
| A valid zip with no `MANIFEST.json` | There is no way to tell what it holds; exports always carry one. |
| `format_version` above what is understood | Names both versions, and says to update the app. |
| `format_version` below 3 | Names it as written by a development build before the container was split in two. **No released version wrote one.** D96. |
| `encrypted: false` | Says what the file is, a complete readable copy of the notebook, and refuses it. D67. |
| No passphrase offered | Says one is needed and that it cannot be recovered. |
| Wrong passphrase, or altered file | Says it cannot tell which, and why. |
| A payload with no final frame | Says it was cut short, and refuses to hand over a partial record. |
| A file that does not match `CHECKSUMS.txt` | Names the file. |
| A payload that is not SQLite | Says the passphrase was right and the file is not damaged, and names the cause. |
| An attachment whose bytes do not match its name | Names it as damaged. |
| An attachment referenced by the database and missing from the archive | Names it. |
| A table or column this build does not have | Names it, rather than importing only what it recognizes. |
| A manifest whose row counts disagree with the database | Names the disagreement. |

---

## 10. What the round trip test asserts

Not "did the export succeed". Field by field equality after export, wipe, and import:

- every row of every table, including tombstones
- all archived, pinned, resolved, and completion state
- ordering
- all timestamps to the millisecond
- every relationship between rows
- every attachment present and hash verified
- view preferences and the conflict log
- the schema version

It runs on a fresh install, on an install with existing data, and on a device with less storage than the source.

**It runs in continuous integration and on the connected phone.** An earlier version of this line said "on an emulator, never on the owner's device", which is stale: the emulator was dropped from this project in D21, D23, and B4. Data survival is proven by this round trip running on every push, which is repeatable, rather than by preserving one phone's installation, which is a sample of one nobody can reproduce.

On the phone the standing rule applies, and it is a checklist step rather than a reason to avoid anything: `connectedAndroidTest` uninstalls the application and takes its data with it, so anything worth keeping is exported through the app first and reimported after.

**And the regeneration test, which is the important one.** Export, import onto a clean install, regenerate the readable copy from the imported database, and assert it is byte-identical to the readable copy inside the original archive. Because the readable copy renders every field, any value lost, shifted, reordered or re-derived anywhere in the round trip changes the output and fails the test. One assertion, near-total coverage. `contract/DATA-CONTRACT.md` 8.5.

**No feature is finished until it survives that round trip.**

---

## 11. Provenance

Everything here is AGPL-3.0, like the rest of Health Trail. Copy it, change it, pass it on. That license is part of the promise: a specification somebody can be prevented from reading is not one this document could honestly claim to be.
