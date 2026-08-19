# What eleven panels found, 2026-08-18

Nothing here has been executed. The detail, panel by panel, is in `docs/findings/`.

**Read this before acting on any of it.** The four earliest panels were told to read the Kotlin, so they answered as engineers. The owner corrected the framing and the real user panels were re-run with no repository access at all. Both are kept: the engineering panels found true defects, the user panels found different ones. Where the main session re-checked a claim and the panel was wrong, the correction is inline and marked.

---

## 1. The app can lose everything, silently. This outranks all other work.

Five failure paths, each verified. They compound: 2 causes the corruption, 1 deletes the notebook, 3 destroys the recovery, 4 makes the remedy uninstall.

**D1. On corruption, the database is deleted and the app opens as a fresh install.** `HealthTrailDatabase.kt:101` passes `null` as the fourth argument to `openOrCreateDatabase`, which is the `DatabaseErrorHandler`. There is no handler anywhere: `DatabaseErrorHandler`, `onCorruption` and `integrity_check` return zero hits across the whole codebase. The library therefore uses `DefaultDatabaseErrorHandler`, whose `onCorruption` deletes the file. Next launch sees no file, runs `applySchema`, and shows "Before you start". **Five years, gone, with no error and nothing on screen.** Confirm the exact library behavior on the phone; the null argument and the absent handler are already facts.

**D2. The journal mode the contract declares has never been applied, by either route.** `contract/schema.sql:83` declares `PRAGMA journal_mode = WAL`. `applySchema` runs it through `rawQuery` **inside** `beginTransaction()`, which SQLite refuses, and `.use { it.moveToFirst() }` discards the answer, so nothing reports the failure. Separately, the AOSP-derived connection pool re-applies its own defaults on every connection open. The app runs rollback-journal at `synchronous=NORMAL`, where SQLite does not fsync before overwriting pages. `DatabaseKey.kt:97` deletes `-wal`/`-shm`, so the project believes WAL is on. It is not.

**D3. Restore replaces the live database with a stream copy, not a rename.** `Backup.kt:509` is `rebuilt.copyTo(live, overwrite = true)`. Process death inside it leaves a truncated database beside a complete `.replacing` file that nothing ever reads. The person loses everything at the exact moment they were recovering it.

**D4. Anything but a lost key crashes the app forever.** `AppRoot.kt:98` catches only `DatabaseKeyLost`. A failed migration, a sideloaded older APK, a disk-full `SQLiteException` all escape uncaught and crash at launch on every start. The data is intact and unreachable, they cannot even export it, and the remedy they will reach for is uninstall.

**D5. Restore writes attachments after the database swap, outside any transaction.** `Backup.kt:539-542`. Interruption leaves rows pointing at photographs that never arrived, and the app renders a lost file as *"No photograph of this one yet"*. **The person is told they never took the picture.** `MergeApply.kt:116-121` does this the correct way round; restore does not.

**And the backup is not proven.** Export never reads back what it wrote (`NotebookShell.kt:1105`), so a truncated file still says "Saved". `readablePages` swallows any failure into `emptyMap()` (`ExportContainer.kt:834`), so the human-readable half can be silently absent from a successful export. `Attachments.verify()` has zero callers, so a rotted file is exported corrupt and only detected on the new phone with the old one gone. Nothing records when the last export happened. **Nothing has ever been restored and read back on the person's own phone.**

Five gaps with no version at all: no integrity check ever; no crash-recovery adoption of `.replacing`/`.arriving`/`.part`; no evidence the backup works; no second copy on the device; no free-space check anywhere.

## 2. Security: two real holes on the import path

**S1. Zip path traversal, and the panel's own fix was insufficient.** `ExportContainer.kt:1113-1118` joins attachment entry names to the staging directory with no containment check and writes before any checksum runs. `requireSafeName` guards only the write path. **The main session checked and `requireSafeName` does not reject `..`**, it tests ASCII, length, `<>:"|?*\` and reserved device names, all of which `../../shared_prefs/health_trail_key.xml` passes. Only a canonical-path containment check closes this. Destroying the wrapped key makes the notebook permanently unopenable. **High, not critical:** there is no network, so it needs someone handed a malicious archive *and* the attacker's passphrase. Realistic here, not remote.

**S2. Views from a restored archive reach unquoted SQL.** `unknownShape` validates `type = 'table'` only. `sqlcipher_export` copies views in, `userTables` then reads `type = 'view' AND name LIKE 'live_%'`, and `recomputeRanges` interpolates the name and column prefix into `rawQuery` with no bound arguments. Impact is bounded to roughly a crash mid-restore, but the gate has a hole. **Fixing it needs care: every legitimate archive carries the `live_*` views, so it must whitelist the expected set, not reject views.**

Also: no decompression limits on either zip layer; the restore staging directory is never deleted, leaving a **plaintext** SQLite copy of the whole record in cache after any restore, including a failed one; nothing sets `FLAG_SECURE` or disables the recents thumbnail.

**What is genuinely sound, do not "improve" it:** Argon2id at 64 MiB / 3 passes with AES-256-GCM, correct frame nonces with index and finality bound into the AAD, authenticate-before-decrypt, a non-exportable Keystore-wrapped database key, 123 SQL sites with zero unsafe value concatenation, no permissions at all, `allowBackup=false`, and exactly one `Log.` call in the shipping tree.

## 3. Silent write loss in ordinary use

- A document saved with a blank title **discards the photograph and closes the screen**, no write, no error (`NotebookShell.kt:2368`).
- A prep-sheet answer sets two racing effects, and `markQuestionAsked` unconditionally writes `answer_text = NULL`, **erasing the answer just typed**.
- `resolution_note` has no writer, and reopening an incident re-runs the same `UPDATE`, **erasing any note that existed**.
- `storePicked` collapses disk-full, revoked permission and OOM into one message that says the file is over 25 MB. **Wrong cause, and the implied remedy fails identically.**
- All 77 write paths live in `LaunchedEffect` over plain `remember`, so process death between Save and the write loses the draft with no trace.
- `makeSubjectActive` is two writes with **no transaction**, under a comment claiming one. `createSubject` sets `is_active = 1` explicitly, so adding a person creates a second active row before the clear runs. Crash between the writes and there are zero active subjects: **the notebook opens empty and the person believes their records are gone.** No test exercises either function.
- Same shape in `recordMedicationEvent` and `moveToChapter`, both under comments claiming a transaction that is not there.
- `recomputeRanges` updates every dated row without touching `updated_at` or `rev`, appending thousands of change-log rows at stale revisions on every restore.

## 4. What the fourteen users actually said

Plain language, no code. The recurring ones, in their words:

**They cannot hand anyone a slice.** Every single panel hit this. *"I could not pull out everything since the last meeting."* *"I could either send her the whole export or nothing."* *"Everything has my private opinions of her colleagues in it."* Five fixed artifacts leave the app and nothing else does.

**Counting.** *"Six laundry complaints in four months, and nowhere does the app say this is the sixth time, and that is the whole point I am making to them."* *"Getting to she fell four times since Easter meant counting them myself by scrolling. Counting is the one thing I want the app to do and it made me do it."* Rule 2 permits this: it reads *record, organize, count*. Only a total presented as a verdict, a delta or a direction stays forbidden.

**Two numbers, one box.** Blood pressure and pre/post dialysis weight. *"I have been typing 140 over 85 into the note for four months and now none of it lines up."* One user gave up tracking entirely. Needs schema, so it is yours.

**Repetition.** *"I have typed the same clinic name over a hundred times."* *"Every incident asks me to start from nothing. It is the same incident every time."*

**Things that are obviously one thing.** A bill, the letter about it, and the call about it. A missed dialysis session and the ride that never came. An appointment and the exercises given at it. The losing of a hearing aid in March and the finding of it in April.

**Two users defended rule 2 unprompted.** *"If it started grading her I would not trust anything else in it."* Do not weaken it.

**One thing they asked for that you should refuse politely:** the app noticing patterns for them. Counting is arithmetic and allowed; noticing is interpretation and is not.

## 5. The structural cause under 114 complaints

The schema is far richer than the interface. Columns with a **reader and no writer** are screen sections that are permanently, structurally empty. The sharpest: `documentsOnIncident` inner-joins `document.entry_id`, and **`createDocument` never writes it**, so an incident's Documents section is empty for every person forever. `attachment` has exactly one writer setting only `document_id`, orphaning `entry_id`, `bill_id`, `measurement_id`, `person_id` and `project_id`. `standing_instruction.given_to_person_id`, `medication.prescriber_person_id` and `measurement.reported_by_person_id` have neither writer nor reader. `attended_*` and `outcome_note` are read by search and written by nothing. `care_thread.ended_edtf` has zero writers. `cost_sheet` and `cost_entry` are whole tables with no code.

Filing is write-once at capture: an entry can never be moved to another thread, joined to a project later, or split. A memo can never be re-targeted. `recordMeasurement` never puts the value on the entry it writes, so **a shared month prints "Weight" with no number**.

The moderator's ranked closing list is in `docs/findings/engineering-r3-moderator.md`.

## 6. Your three observations, traced and answered

**Headers.** Seven implementations. `Page.kt` covers 62 screens; `TodayField`, `Notebook`, `OneThread` and `MedicationList` hand-roll their own; `ProjectsScreen` has no app bar at all and draws its title as the first row of the list; the four onboarding screens use a third size in a plain `Column`. Two scrolled surface colors, four title treatments. **Answer: one `PageBar`, the only thing in the app that draws a screen title. Twelve files.**

**The lamp not moving with the title.** Structural: `LargeFlexibleTopAppBar` pins `actions` to the top row and puts the large title on the second, so every intermediate scroll offset draws them apart. **Answer: move `HeaderActions` into the `title` slot as the trailing element of a `SpaceBetween` row, leaving `actions` for the badge.** They then collapse as one row, which is what Today does. Two files. The experts split on this and the researcher broke the tie: it is the only option where the alignment cannot drift again.

**Empty states.** 23 `SectionEmpty` sites, 8 pass no action, and six of those have a real action. **Answer: `Page` takes one `PageAction`, renders it as the FAB, and publishes it so `SectionEmpty` reads it as its default, a screen then cannot have a FAB and an actionless empty card.** Eleven files plus four locale catalogs. Bin and Search stay genuinely actionless.

## 7. Design and motion, beyond the three

`PullToRefreshBox` wraps all 62 `Page` screens and `onRefresh` has **zero callers app-wide**: the gesture does nothing everywhere. Roughly forty detail destinations are bare `if` blocks, so every push and pop is a hard cut. The capture menu appears and vanishes in one frame. The staged capture form cuts between stages. Two complete press systems coexist and six `indication = null` sites remain. Reduced motion is read once with no observer, so **turning it on while the app is open does nothing until the process restarts**, which is exactly how rule 19 is verified. Six card sites put the ripple outside the clip, so it draws square over a 26dp corner on every tap.

Repeats across screens: the `Lead` composable is called by 0 of 27 screens; `labeledBlock` is reimplemented by hand at 14 sites; the same "date, title, three-line body" card exists as 7 private composables that already disagree; nine vertical gap tokens across 74 spacers; four divider insets. `MoneyScreen` totals mixed currencies as one number.

## 8. What is genuinely good, and must not regress

The keep-both merge is the strongest thing in the codebase: id-matched, `updated_at` with an `origin_device` tiebreak so both phones reach the same answer, never deletes, all-or-nothing on a dangling reference, losing versions written whole to `conflict_log`. Deletion is always a tombstone and no hard `DELETE` exists anywhere. The change log is trigger-enforced in the schema, not by discipline. No base-table read escapes a `live_` view across 311 files. Rule 2 holds under real pressure on Progress and Measure. Rule 17's editable-forever date works where it was built. Attachments are content-addressed and published by atomic rename. Subject scoping holds at the query layer; the two unscoped queries (`ownTemplates`, `organizationNamed`) hold shared reference data, not one person's records, and the dedup reads as deliberate. Dictation's rule is centralized in one field across 34 call sites.

## 9. Contested, and what needs you

Resolved by rule 23 and recorded in `docs/findings/engineering-r3-moderator.md`. Four still need you:

1. **A family per-dose log**, when `contract/schema.sql:531-533` says the app deliberately does not track doses. Yes or no.
2. **Paired readings.** Blood pressure and pre/post weight need a second numeric column or a companion-row convention. Schema, so yours.
3. **Indexes.** 45 foreign-key columns have no index; at five years each detail screen scans child tables end to end. Schema, so yours.
4. **The legacy press system.** Delete `ui/components/Press.kt` outright, or keep its press scale and focus ring. Visible, so yours.
