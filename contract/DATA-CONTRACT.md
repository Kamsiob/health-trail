# Health Trail Data Contract

This document is binding on Phase 0. Everything in it must be in place before any feature is built on top of the database, because each requirement here is cheap now and a rewrite later.

Health Trail ships on Android first. A web version and possibly a Linux desktop version follow, and the Android app must be able to sync directly with a computer over a local connection with no server in the middle. None of those are v1 features. All of them are v1 constraints on the data model.

The one sentence version: **the database and the export format are a public contract between three platforms and a future sync engine, so they get designed once, correctly, at the start.**

**Amended 2026-08-03 by owner decision, recorded as D83.** Section 8 was replaced in full by **THE ARCHIVE**, which is a substantially stronger export and import specification than the one this document previously carried. The amendment is binding, it arrived with design direction v4, and it is the one part of that direction that extends this contract rather than being subordinate to it. Section 9 was corrected to match, because a backup and an export are now the same artifact.

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
  EXPORT-FORMAT.md the export container specification
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

**Row metadata is not an event date.** The five timestamps above are UTC milliseconds and stay that way: they are bookkeeping about the row, not claims about the world. When something actually happened is a different kind of value entirely, and section 3.1 governs it.

### 3.1 Event dates

**The owner approved this on 2026-07-31 and it is made before real data exists**, because retrofitting it later means discarding records. D34.

**The problem it solves.** A care record spans years and is written from memory. "The fall was sometime in November 2024." "She was moved to Brookdale in the fall." "I called them, I think it was a Tuesday." A schema that stores only a precise timestamp forces every one of those into a false precision, and once it has been exported and reimported there is no way left to tell a guess from a known time. The app would then assert a specific Tuesday the person never claimed, which breaks its own rule about never saying more than it knows.

**The standard is EDTF**, the Extended Date/Time Format, standardized as ISO 8601-2:2019 and developed by the Library of Congress for exactly this. https://www.loc.gov/standards/datetime/edtf.html **No private format is invented here.** Two properties of it are load bearing:

- **Precision is expressed by truncation.** `2024` is sometime in 2024. `2024-11` is sometime in November. `2024-11-18` is that day. `2024-11-18T14:40` is that moment. The shorter the string, the less the person claimed.
- **Uncertainty is a separate axis from precision.** `?` marks uncertain, `~` approximate, `%` both. A person can know the month and be unsure of it, which is not the same as knowing only the year. Collapsing the two into one field throws away information the person actually gave.

**The columns.** Wherever a table records when something happened, it carries this group, named for the event: `occurred`, `scheduled`, `started`, `ended`, and so on.

| Column | Type | Rule |
|---|---|---|
| `<name>_edtf` | TEXT | **The source of truth.** A valid EDTF string carrying the local wall-clock reading, with no offset suffix. This is what round-trips through export and import unchanged. |
| `<name>_zone` | TEXT NULL | The IANA zone the person was in when they recorded it, for example `America/New_York`. Null where the precision is coarser than a day, since a month has no clock. |
| `<name>_start` | INTEGER NULL | **Derived.** The earliest instant the date could refer to, UTC milliseconds. |
| `<name>_end` | INTEGER NULL | **Derived.** The latest instant it could refer to, UTC milliseconds. |

**The range is an index, never the truth.** It exists so the database can sort, filter, and answer range queries efficiently. It is recomputed from the EDTF string and is never edited independently. A known moment has an identical start and end. Sometime in November 2024 spans that whole month. **If the two ever disagree, the EDTF string wins and the range is wrong.**

**Local wall-clock semantics are preserved, and the EDTF string is how.** A visit logged at 2:40 pm happened at 2:40 pm where the person was, and it still reads as 2:40 pm after a timezone change or after travel, because the stored string says `T14:40` and not an instant. The zone is kept so the derived range can be computed correctly and recomputed identically later. This is deliberately different from the row metadata above, where `updated_at` and `deleted_at` correctly stay UTC milliseconds.

**Every supported precision, with its canonical form.**

| What the person said | Stored | Resolved range |
|---|---|---|
| An exact moment | `2024-11-18T14:40` | that minute, start equals end |
| An exact day | `2024-11-18` | that day, local midnight to midnight |
| A week | `2024-11-18/2024-11-24` | that interval. EDTF has no week token, so a week is an interval, which is what a week is |
| A month | `2024-11` | that month |
| A season or part of year | `2024-21` spring, `2024-22` summer, `2024-23` autumn, `2024-24` winter | that sub-year group |
| A year | `2024` | that year |
| A range between two points | `2024-11/2024-12` | first instant of the start to last instant of the end |
| Genuinely unknown | `XXXX-XX-XX` | both null |
| Any of the above, unsure | the same with `?`, `~`, or `%` | unchanged. **Uncertainty never widens the range**, because being unsure about November is still a claim about November |

**Null and unknown are different things, and conflating them is the mistake this table exists to prevent.** A null `<name>_edtf` means **the event has not happened and there is no date to record**: a chapter with no end, a bill not yet paid, a question not yet asked. `XXXX-XX-XX` means **it happened and nobody knows when**. A bill that was paid at some forgotten point is not an unpaid bill, and the schema must not be able to say it is. Where an event definitionally happened, the column is `NOT NULL DEFAULT 'XXXX-XX-XX'`: an entry, a measurement, a milestone, and a violation all occurred, so the only question is how much is known about when.

**Unknown is a first-class value, not a null to work around.** `XXXX-XX-XX` is EDTF's own unspecified-digits form and it means what it says: a date exists and nothing about it is known. An entry carrying it **saves, is valid, and appears in the trail**. It is never blocked, never hidden, and never quietly filled in with today. Because its range is null it cannot sort among dated entries, so it sorts by `created_at`, which is when it was written down, and it renders as not known rather than as a guess. **It never disappears from a view because its date was vague**, and a search for a month returns every entry whose range overlaps that month plus nothing that merely might.

**The round trip requirement.** `<name>_edtf` survives export and import byte for byte. The derived columns are recomputed on import rather than trusted from the file, which is what makes them an index rather than a second source of truth. The field by field round trip test in section 8 covers the EDTF column directly and asserts equality on it.

**Golden vectors.** `contract/test-vectors` carries a case for each row of the table above, with its EDTF string, its resolved range, and its rendered form in all four locales. Both platforms run them. **A renderer that turns `2024-11` into "November 1, 2024" fails the vector**, which is the point of having one.

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
- **Every date precision in section 3.1**, with its EDTF string, its resolved range, and its rendering in all four locales. A renderer that turns `2024-11` into "November 1, 2024" fails the vector, which is the point of having one. The file is `dates.json` and it is the first vector both platforms run, because every other vector's output contains a date.

---

## 8. THE ARCHIVE

**Owner-approved amendment, 2026-08-03, recorded as D83.** This section replaces the export container specification this document previously carried. It is binding, and **any gap in it is a release blocker.**

**Two failures would be worse than any bug in this app**, because both destroy years of a person's work with no recovery and no server to fall back on.

**The first:** a person exports after three years, opens the file, and cannot read it. **Nothing in this app matters if the record it produces is only readable by this app.** A caregiver's archive outlives the phone, outlives Android, and very likely outlives this project.

**The second:** a person exports, gets a new phone, imports, and the record comes back subtly wrong. Entries attached to the wrong person, attachments missing, dates shifted by a day, pins gone, a resolved incident open again. **Silent partial correctness is worse than an honest failure**, because the person will not notice for months and by then the source device is wiped.

Everything below exists to make both impossible.

### 8.1 The container, which is two layers

**Every export is encrypted with a passphrase. There is no unencrypted export path, no chip offering one, and no settings toggle producing one.** D67 stands, confirmed by the owner on 2026-08-03 and recorded as D84.

**The requirement that replaces the unencrypted option is harder and better: an encrypted archive must remain openable by someone who has the passphrase but does not have this app.** A format only this app can decrypt is the same failure as a format only this app can read, arriving one step later.

**So the container is two layers, not one.**

#### The outer layer

**A plain, unencrypted ZIP64 archive**, so it does not break past four gigabytes or past 65,535 entries. Named so it sorts and identifies itself without being opened, for example `health-trail-2027-03-14.zip`. It contains **exactly three things**:

```
README.txt      plain UTF-8, ASCII only, readable by anyone who opens the file
MANIFEST.json   the non-sensitive header only
payload.enc     the encrypted archive described below
```

**Nothing in the outer layer reveals anything about the person or their record.** The outer `MANIFEST.json` carries **only**: format version, app version, schema version, export timestamp, the encryption algorithm, the key derivation function, and every Argon2id parameter needed to derive the key again in ten years. **No names, no counts, no dates of care, and no locale that would narrow down who this is.**

**The export timestamp stays, and the filename stays human-recognizable.** Decided under `CLAUDE.md` rule 23 and recorded as D86. Removing the date would **protect nothing**, because the filename already carries it, while costing the person the ability to tell six backups apart. And 8.6 requires that nothing is written before the person sees what a file holds, which a file whose date is unreadable until after decryption cannot satisfy. **An export timestamp is a fact about the file. A date of care is a fact about the person.** That distinction is what the outer layer is drawn around.

**The outer `README.txt` is written for a stranger who found this file and has the passphrase.** It states what the file is, that the contents are encrypted, exactly which algorithm and parameters were used, where the format is documented, and **how to decrypt it without this app**. It states plainly that **a lost passphrase means the archive is unrecoverable, with no server, no backdoor, and no recovery path anywhere.** It names `tools/decrypt/` and gives its repository path.

#### The inner layer

Once decrypted, `payload.enc` is the full container, unchanged:

```
README.txt          plain UTF-8, ASCII-only characters, no markup
MANIFEST.json       the machine header
CHECKSUMS.txt       SHA-256 of every other file, one per line
data/trail.sqlite   the complete record, the file the importer reads
data/schema.sql     the schema this file was written against, as DDL with comments
readable/           the human copy, described in 8.2
attachments/        the original bytes of every photo and document
```

**The inner `README.txt` is the first thing a stranger reads once inside, so it is written for a person and not for a developer.** It says what this file is, who made it and when, which folder to open to just read the record with no software at all, which file to give back to the app, and what the format version is.

**The inner `MANIFEST.json` carries:** format version, app version, schema version, export timestamp as UTC epoch milliseconds plus the IANA timezone id of the device, the locale the readable copy was written in, a row count per table, a file count and total byte size for attachments, and **the list of any attachment whose bytes could not be read at export time.** This is where the identifying detail lives, which is why it is inside the encryption rather than outside it.

#### Three requirements that make the outer promise real rather than aspirational

1. **The format is published.** `contract/EXPORT-FORMAT.md` specifies the container **byte for byte**: header, salt, nonce, Argon2id parameters, cipher, and layout. It lives in this repository under AGPL like everything else, **so it survives this project.**
2. **A standalone decryption tool ships in the repository** at `tools/decrypt/`, written in a language with **no build step and no dependency beyond a standard Argon2 and AEAD library**, with a README somebody who does not write software can follow. **It is tested in continuous integration against a real archive on every change to the export code.**
3. **The passphrase gets every chance to survive.** At export the person confirms it **twice**, and the screen tells them, in the app's voice and without lecturing, that this passphrase is the only key and belongs somewhere that is not the phone. They may optionally write a **hint**, stored in the **outer** `MANIFEST.json` in plaintext, so **the app must say plainly that the hint is readable by anyone who has the file and must never contain the passphrase itself.** Automated local backup reuses a passphrase the person set once, so the recurring backup is never blocked by a prompt they will not see.

**SQLite is the payload** because it is one of a small number of formats the US Library of Congress lists as recommended for long-term preservation of datasets, and because the file format is stable, public domain, and readable by every platform this project will ever target. That choice was already in this contract and it is correct. **What the contract was missing is that SQLite has no place to record what its tables mean** beyond the CREATE TABLE statements, which is exactly why `MANIFEST.json`, `data/schema.sql` with real comments, and the readable copy all ship beside it.

**Tombstones are included.** An export that drops them cannot restore a deletion, which means restoring a backup resurrects things the person deleted.

### 8.2 The readable copy

**The standard is HL7 Clinical Document Architecture's**, which has required for twenty years that a clinical document be renderable by any recipient using general-market tools with no special stylesheet shipped alongside it, and that the complete attested content be present in the human-readable form rather than only in the machine entries. **Apply exactly that standard here.**

**Completeness.** Every field stored in the database appears somewhere in the readable copy. **It is not a summary, not a highlight reel, and not a subset.** If a column exists and nothing renders it, that is a defect, and 8.5 makes it a build failure.

**No dependencies.** Plain HTML. No JavaScript. No web fonts. No external stylesheet, no CDN, **no network request of any kind.** One small CSS block inlined in each page so every page stands alone even if separated from the others. System font stack only. **It must render correctly on a browser that does not exist yet**, which means using nothing clever.

**Structure.** `readable/index.html` is the front door: who this record is about, the date range it covers, the counts by section, and a table of contents linking to every other page. Then **one page per section per year**, so no single page becomes unopenable at year five. **Attachments are referenced by relative path into `../attachments/`, never embedded as base64**, because a three-year archive inlined into one file will not open.

**Printable.** A print stylesheet, so a person can print a year, or one incident thread, and hand it to a doctor, a lawyer, or a sibling. **Page breaks land between entries, never inside one.**

**Faithful to the record and to the app's rules.** Same content rules as the app: no ranges, no interpretation, no color-coded values, no conclusions. **Gaps in a measurement render as gaps.** A field the person never filled reads as **not recorded**, never as zero and never as blank.

**Faithful to the person's language.** Written in the locale the person used the app in, with the correct `dir` attribute and correct RTL rendering for Arabic, **verified in a browser and not assumed.**

**Dates.** Every date renders in a form a stranger reads without ambiguity, showing the local date and time as the person experienced it plus the UTC offset. **Never a bare epoch number. Never a locale-ambiguous numeric date such as 03/04/2027.**

**Cross-referenced.** Every entry in the readable copy prints its id, and that id is the same id in `data/trail.sqlite`, so a person or a future tool can move between the two halves of the archive by hand.

**Deterministic.** The same database produces **byte-identical** HTML every time. The generation timestamp lives in one named place and nowhere else. **8.5 depends on this.**

### 8.3 Import must map, not merely parse

**Import is not "did the file open." Import is "is the record on the new phone the same record."**

**Verify before writing.** Read `MANIFEST.json` first and refuse an unrecognized format version by name, stating what was found and what is supported. Then verify every checksum in `CHECKSUMS.txt`. Then verify that every attachment row in the database has a matching file and that every file matches its recorded hash. **Only then begin writing.**

**Atomic.** It fully succeeds or changes nothing. **There is no partial import and no half-restored state, ever.**

**Identity by id, never by position.** Every row carries the UUID it was created with. Import matches on that id and on nothing else. **Never match by name, by row order, by index, or by any value the person can edit.** This is the single most common way a round trip silently produces a wrong record, and the schema already prevents it, so the importer must not reintroduce it.

**Nothing is invented at import.** No new ids. No refreshed timestamps on existing rows. No re-derived ordering. No defaulted values filling a null. **If the file does not say it, the import does not decide it.**

**References resolve or the import stops.** If any row points at a parent that is not present in the file, the import halts before committing and names the row and the missing parent in plain language. **It never quietly drops the child and it never quietly creates a placeholder parent.**

**Attachments relink.** Every attachment is written to storage and its row is repointed at the new path, then verified by hash. **An attachment listed in the manifest as missing at export time is imported as a known-missing attachment with its name and date intact**, so the person sees that a photo existed and is gone, rather than never learning it was there.

**State survives.** Tombstones import as tombstones, so a deleted thing stays deleted. Pins, archived state, resolved state, completion state, and the person's per-section view choices all restore. **A resolved incident that reopens on a new phone is a failure.**

**Merge or replace is an explicit choice**, described in plain words, never a guess. Merge matches by id, resolves by the later `updated_at`, and **writes every resolution to a conflict log the person can actually open and read.**

**Unknown content is named, not dropped.** If the file contains a table or a column this version does not recognize, say so and name it. **Never silently discard part of someone's record.**

### 8.4 The named failure modes

These are the specific mechanisms that corrupt round trips in practice. **Each one gets its own test**, not a general confidence that it is handled.

**Time.** Store every timestamp as UTC epoch milliseconds plus the IANA timezone id in effect where the entry was made. **A note written on July 6 must still read July 6 after the person moves to another country.** Test import on a device set to a different timezone, on a device whose clock is wrong, and across a daylight saving boundary in both directions.

**Unicode.** Normalize all text to NFC on write and compare NFC-normalized on import. **A name typed with a combining accent on one device and a precomposed character on another is the same person, not two.** Test with Spanish accents, Arabic, and Chinese.

**Filenames.** Write ZIP entries with the UTF-8 flag set, which is bit eleven of the general purpose flag field. **Do not rely on it being read correctly**, though: name every file inside `attachments/` from its attachment id using ASCII characters only, and carry the person's original filename in the database and in the readable copy. That removes the entire codepage class of failure and the case-collision class at once. **No two entries may differ only by case. No path longer than 180 characters. No reserved Windows names, no trailing dots or spaces, no colons or backslashes.**

**Ordering.** Every query that feeds an export or a render has an explicit `ORDER BY` on stable columns. **Never depend on rowid, insertion order, or default iteration order.**

**Numbers.** Money as integer minor units, never floating point. **A measurement stores both the text the person typed and the parsed value, and both survive the round trip**, so 5.0 does not come back as 5 and a value the app could not parse is not lost.

**Absence.** Null, empty string, and zero are three different things and stay three different things. **Not recorded is never rendered as a number.**

**Scale.** Test at the five-year fixture: thousands of entries, hundreds of attachments, an archive past four gigabytes, and more than 65,535 entries. **Confirm ZIP64 on both the writing and the reading side** rather than assuming the platform handles it.

**Encryption.** The export passphrase derives the export key, **independently of the device database key**, which cannot travel and which has already broken this app's exports once. Record the derivation parameters in the **outer** `MANIFEST.json`, unencrypted, so a future version or a stranger with the passphrase can still open an old file.

**Encryption is the largest long-term risk to readability**, since a technical protection mechanism is precisely what preservation practice warns against. **The answer here is not to offer a plain file**, which D67 removed for good reason and which the owner confirmed on 2026-08-03: the payload is now a plain SQLite database, so an unencrypted container would be a fully readable copy of an entire care record sitting in a folder a file manager can browse and a cloud sync can copy. **The answer is 8.1's two layers**: publish the format, ship a standalone decryption tool that does not need this app, and give the passphrase every chance to survive. **The export screen states plainly that a lost passphrase means a lost archive with no recovery.**

### 8.5 The tests

**The regeneration test, which is the important one.** Export an archive. Import it onto a clean install. Regenerate the readable copy from the imported database. **Assert it is byte-identical to the readable copy inside the original archive.** Because the readable copy renders every field, any value lost, shifted, reordered, or re-derived anywhere in the round trip changes the output and fails the test. **One assertion, near-total coverage.**

**The coverage test.** Enumerate every column in `contract/schema.sql` and assert each one appears in the readable renderer's field map. **Adding a column without rendering it fails the build** until it is either rendered or explicitly listed as not-for-rendering with a written reason. This is what keeps completeness true in year three rather than only on the day it was built.

**The field-by-field round trip**, which this contract already required, stands, **extended to** attachments verified by hash, tombstones, pins, view preferences, and the conflict log. It asserts on every row of every table including tombstones, all archived, pinned, resolved, and completion state, ordering, all timestamps to the millisecond, every relationship between rows, and the schema version. It runs on a fresh install, an install with existing data, and a device with less storage than the source. It runs on an emulator, never on the owner's device.

**The offline read test, which is the whole two-layer promise as a single procedure.** On a machine with **no network** that has **never had this app installed**:

1. **Extract the outer layer with a general-purpose zip tool.** Not the app, not a script this project wrote for the purpose. Whatever the machine already has.
2. **Decrypt `payload.enc` using only `tools/decrypt/`**, following its README as somebody who does not write software would.
3. **Open `readable/index.html` in a browser.**

**Everything must render, every link must work, every attachment must open.** Run it in each of the four locales, and confirm the Arabic copy reads right to left.

**That full path is the test, and it is what actually proves the archive outlives the app.** A test that starts from an already-decrypted folder proves only half of it, and the missing half is the one that fails in ten years.

**The cross-device test.** Export on one device, import on a **different** device with a different timezone, a different system locale, and a different Android version. **This is the actual new phone scenario and it is not covered by any test that runs on one device.**

**The shape test.** Run all of the above at empty, one, a few, and many, since an export of an almost-empty notebook and an export of a five-year notebook fail in different ways.

**The stranger test, run by a person and not by code.** Hand the readable copy to someone who has never seen the app and ask them what happened in a given month. **If they cannot answer from the archive alone, the readable copy has failed regardless of what the tests say.**

**No feature is finished until it survives all of this.** A feature that stores something the archive does not carry is a feature that silently loses user data on device migration, which for this audience is the worst failure the app can have.

### 8.6 The two screens

Export and import are screens under design direction v4, not system dialogs. Their shapes are in `DESIGN.md` section 14. What this contract fixes about their behavior:

- **Export:** the one thing is a plain description of what the person is about to receive, in the app's voice, **naming that it contains a copy they can read without the app.** **There is no encryption choice, because there is no unencrypted export.** What the screen asks for instead is the passphrase, **confirmed twice**, with one quiet line saying this passphrase is the only key and belongs somewhere that is not the phone. The optional hint is a single field, and beside it the screen says plainly that **anyone who has the file can read the hint, so it must never contain the passphrase.** One filled action.
- **Import:** **nothing is written before the person sees what the file holds.** Show the date range, the counts by section, the attachment count, and the version, then the merge or replace choice as chips, then one filled action. **Progress is honest and cancellable at any point before the commit.**
- **Failure speaks plainly.** Name the file, name what was wrong with it, and say what the person can do. **Never a stack trace, never an error code alone, never a message implying the person did something wrong.**

---

## 8.7 What a person arranges is record, not preference

**Amended 2026-08-04 on the owner's instruction, with the adoption of the Today and Projects grids.** D110. This is the one place those grids reach past design and into this contract.

**The following are record. They live in the same database, travel in the archive, restore on import, appear in the readable copy, and join the coverage test in 8.5 and the regeneration test.**

| What | Shape |
|---|---|
| **The Today layout** | An ordered list of card instances, each with a **type**, a **size**, and an **optional source id** (which measure, which project, which person), plus **the single lead assignment** |
| **Project templates** | A name, ordered stage names, a lead, starting steps, paper placeholders, and date kinds. **Applied by copy, with no live link back** |
| **Project stage assignments** | Which stage a project stands in, and when it moved |
| **Standing entries** | Whose hands a project is in, since when, and what is happening |
| **Recorded dates with their sources** | The date, its kind, and **the paper or entry it was taken from** |
| **Steps with handler tags** | The step, its cluster, whether it is arranged, and the label naming who is handling it |

**Why this is a contract amendment rather than a preference stored somewhere convenient.** A person's arranged Today is something they made. So is the shape they gave a project over eighteen months. **If those do not survive the new phone, the app has quietly decided that what somebody built is less real than what they typed**, and section 8's whole promise is that everything they made comes back.

**A handler tag is a label and never an identity.** No account, no address, no second user, and nothing about it leaves the device. It is the person writing down who said they would do a thing.

**A source id that no longer resolves is kept, not dropped.** A card pointing at a closed project renders its source-closed state and stays until the person removes it. **Import never silently discards a card because its source is gone**, which would be the file quietly editing somebody's desk.

## 8.8 A note, and the only rich text this app stores

**Decided 2026-08-18 for #397, D207. No table was added and no column changed.**

**A note is an entry**, `entry.kind = 'note'`, which the `CHECK` constraint has
allowed since Phase 0. It is not a second trail and not a parallel record: it is
a kind of entry, so it is already on the trail, already in search, already in
the archive, already merged, and already carried by the change log. A separate
`note` table would have been a second answer to a question this schema answered
before the app could write one.

**A note attaches to anything through `link`**, which already exists and is
already bidirectional by construction: `source_table`, `source_id`,
`target_table`, `target_id`. A note about Tuesday's visit is one row,
`('entry', <note id>, 'appointment', <appointment id>)`, and both sides read it
with the same query the project trail already uses. **Rule 18 is a property of
the table rather than a thing each screen has to remember.**

**A note with no target is a note with no link row.** That is the ordinary case
and it is not a deficiency, rule 13.

### 8.8.1 The marks, and there are exactly three

**Rich text is stored in `entry.body` as plain text carrying a fixed, named
subset of Markdown.** Not HTML, not a document model, not a serialized editor
state.

| Mark | Written | Means |
|---|---|---|
| Bold | `**text**` | bold |
| Italic | `_text_` | italic |
| Bullet | a line beginning `- ` | one item of a list |

**Nothing else is a mark.** No headings, no links, no tables, no code, no
nesting, no numbered lists, no strikethrough. A `#` at the start of a line is a
`#`. The owner's words were "nothing too crazy", and a subset that cannot grow
by accident is what keeps the archive readable.

**Why text with marks rather than HTML.** The archive is published byte for byte
and a stranger has read it, 8.4. Somebody opening `body` in a text editor sees
`**call the office**` and knows exactly what was written and what was emphasized.
The same string in HTML is `<strong>call the office</strong>`, which is markup a
person has to see through, and it invites an editor to store attributes,
classes and spans that no reader outside this app could interpret.

**The marks survive the round trip because they are the text.** There is no
encode step and no decode step in storage: what the person typed with emphasis
is what is in the column, and export copies the column. **A test asserts the
body is byte identical across export and restore**, which is the only claim that
matters here.

**A reader hears the words and never the marks.** `**` is emphasis for the eye;
for somebody listening, the sentence is the sentence, and the marks are stripped
before the string reaches a content description.

**The readable copy renders the three marks and nothing else**, so the HTML a
stranger opens shows bold as bold, and anything that is not one of the three
appears exactly as typed.

## 9. Automated local backup

Backup matters more here than in most apps, because the people using it are exhausted and the record is often the only continuous account of years of care.

**The automated local backup writes the archive format in section 8, unchanged.** A backup and an export are the same artifact and are covered by the same tests in 8.5. There is no second, weaker format for backups, because the backup is the file the person will actually reach for when the phone is gone, and it is the one that has to be readable in ten years.

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
7. The archive in section 8 writes and reads, with the manifest, the version check, encryption, the readable copy, and **the regeneration test in 8.5** passing on an emulator. The regeneration test is the gate: an archive that round trips field by field but whose readable copy is not byte-identical after reimport has not met this criterion.
8. `/web` opens the same schema through SQLite in WebAssembly and reads the same template JSON, proving the contract.
9. Test vectors exist for at least the empty, single entry, and gap cases, and both platforms run them.
10. Everything in this document that was adapted or deviated from is recorded in DECISIONS.md with the reasoning.
