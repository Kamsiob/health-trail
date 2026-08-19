# The prompt for the next session

Paste everything below the line. It is written to be read once, top to bottom, and acted on in order.

---

Read `gh issue view 321`, then `HANDOFF.md`, then `docs/FINDINGS.md`. All three, fully, before anything else. `docs/findings/` holds the detail behind the third; read a file from it when you reach the work it covers, never in bulk.

## The job

**Fix what eleven review panels found, in the order below. Do not stop until phase 6 is done.**

The app is feature complete. Its screens are built, its export works, its milestone 8 is empty. **What it is not is safe.** A person can use it for five years and lose everything with no error on screen. That is the whole reason this session exists, and it is why the order below is not negotiable: durability, then security, then silent data loss, then correctness, then the wiring that makes records reachable, then the interface.

**Nothing in `docs/FINDINGS.md` overrides `CLAUDE.md`, `contract/DATA-CONTRACT.md`, or a closed decision in `DECISIONS.md`.** Where a finding wants one reopened it is marked OWNER and it waits. Rule 23 does not reopen closed questions.

## Before you touch anything

1. `tools/verify.sh` and record the baseline. **You need to know which tests were already failing**, because 14 were failing on 2026-08-18 and every one is a class `#394` already names. A test that fails after your change and was failing before is not your regression, and a test you assume was already red when it was not is how a real break ships.
2. `git log --oneline -5` and confirm the tree is clean.
3. Confirm the phone is unlocked: `adb shell dumpsys window | grep isKeyguardShowing`. **Never switch Android user profiles**, #316.

## How to work, every single item

**One item, one commit, one push.** Rule 7. Never leave more than one unit uncommitted.

For each item: read the finding's own file for the detail, read the code around it, make the smallest change that fixes it, run `tools/verify.sh`, install, **look at it on the phone**, commit, push. Rule 21.

**Write the test first where the finding is about data loss.** Every item in phase 1 and phase 3 is a defect that produced no error, which means no test would have caught it and no test currently does. A fix with no test is a fix that comes back.

**The owner's standing constraint: breaking the app is not an option.** Every finding carries a RISK-OF-FIX. For anything marked `needs-care` or `risky`, write down what you expect to happen before you change it, then check that it did. For anything marked `SCHEMA-owner-call`, stop and write it to `DECISIONS.md` BLOCKED. Rule 3.

**Three attempts, then move on.** Rule 9. Write what was tried to `DECISIONS.md` BLOCKED and start the next item. Never loop.

**Never ask.** Rule 10. Decide, log the decision, continue.

## Phase 1: the app must stop being able to lose everything

`docs/findings/spec-durability.md`. This phase outranks everything else in the file.

1. **Pass a real `DatabaseErrorHandler`** at `HealthTrailDatabase.kt:101`. It must rethrow and route to `UnrecoverableScreen`, which already exists and already offers restore. **It must never delete.** Today the fourth argument is `null` and the library's default erases the notebook on the first corruption report.
2. **Apply the journal and sync mode for real.** The `PRAGMA journal_mode = WAL` at `contract/schema.sql:83` runs inside a transaction, which SQLite refuses, and the pool re-applies its own defaults per connection anyway. Set it outside any transaction in `create()`, beside the `foreign_keys` line, and **verify on the phone which mode is actually live** before trusting anything downstream of it. Item 3 and `Backup.kt`'s safety copy both depend on the answer.
3. **Make the restore swap atomic.** `Backup.kt:509` is `copyTo`; make it `renameTo`, and adopt `.replacing` on startup when the live file will not open.
4. **Catch `Throwable` at `AppRoot.kt:98`**, not just `DatabaseKeyLost`. Show a screen that names the version and offers export-only. **A migration failure must never end at "reinstall".**
5. **Write attachments before the database swap** in `Backup.kt:539`, the way `MergeApply.kt:116-121` already does.
6. **Make "Saved" true.** After the SAF copy at `NotebookShell.kt:1105`, reopen the destination and run `ExportContainer.open` on it with the same passphrase. Stop swallowing `readablePages` failures into `emptyMap()`. Call `Attachments.verify()` during export and report failures where the screen already renders `missingAttachments`.
7. **Record `last_export_at` and state it as a plain fact** where export lives: "Last saved 14 March". **Rule 13 forbids the nag, not the date.** No score, no percentage, no prompt to do better.

**Gate:** kill the process at each of these points on a real phone and prove the notebook survives. Phase 1 is not done because the code changed; it is done because you interrupted it and the record was still there.

## Phase 2: close the two import holes

`docs/findings/spec-security.md`.

8. **Containment check on every archive entry** before it is written, at `ExportContainer.kt:1113`. Resolve the target and require `canonicalPath` to start with the staging path. **`requireSafeName` is not enough and reusing it will not fix this**, it accepts `..`, which the main session verified.
9. **Validate views and triggers in `unknownShape`**, which today checks `type = 'table'` only, and quote identifiers in `Backup.kt` 403/440/463. **Whitelist the expected `live_*` view set rather than rejecting views, or every legitimate restore breaks.**
10. **Cap decompressed bytes and entry count** on both zip layers, and stream the database and attachments rather than `readBytes()`.
11. **Delete the restore staging directory** in a `finally` after apply. It currently leaves a plaintext copy of the whole record in cache after any restore, including a failed one.
12. **`setRecentsScreenshotEnabled(false)`.** Recents only. **Do not add blanket `FLAG_SECURE`**: it blocks the person photographing their own record, which rule 23 argues against.

## Phase 3: the writes that silently do not happen

`docs/findings/spec-durability.md` and `docs/findings/walks-first-week.md`.

13. A document saved with a blank title **discards the photograph and closes the screen**. Guard the button or write the document.
14. The prep-sheet answer race, where `markQuestionAsked` unconditionally writes `answer_text = NULL` over the answer just typed.
15. `resolution_note` has no writer, and reopening re-runs the same `UPDATE` and **erases any note that existed**.
16. `storePicked` reports disk-full, revoked permission and OOM as "larger than 25 MB". Separate the causes and add a storage-full string.
17. The unknown-length gate at `NotebookShell.kt:3262`, where `-1` passes the size cap and copies uncapped.

## Phase 4: transactions and the contract

`docs/findings/spec-database.md`. **Each of these sits under a comment claiming a transaction that is not in the code.**

18. `makeSubjectActive`, `recordMedicationEvent`, `moveToChapter`, `createPerson`, `applySituation`. Wrap each in one transaction. **Write the test for `makeSubjectActive` first: nothing exercises it today, and its failure mode is a notebook that opens empty.**
19. `recomputeRanges` appends thousands of change-log rows at stale revisions on every restore. **Do not bump `rev` to fix it**, that falsifies the person's own revisions. Suppress logging for the index rebuild, or batch.
20. `clearEveryLeadForTest` ships in the main source set and writes without `updated_at` or `rev`. Move it or fix it.
21. Add a non-suspend `insertRow(database, ...)` for the eight in-transaction callers, so a future suspension point cannot silently move a write outside its transaction.

## Phase 5: make the records reachable

`docs/findings/engineering-r3-moderator.md` holds the ranked list; `docs/findings/users-facility.md` and `users-home.md` are why each one matters. Work its order, best value first. The first five:

22. **Put the reading's value on its entry**, so a shared month stops printing "Weight" with no number.
23. **Add the missing search tables** and re-stamp the incident mirror entry on update and resolve, so a corrected incident is findable by its corrected words.
24. **An appointment needs an "after"**: attended, missed, and one outcome line, printed on the next prep sheet. The missed state needs schema, so log it and build the rest.
25. **Write `document.entry_id`**, so an incident's Documents section stops being permanently empty.
26. **One "File this under" action** on the entry and in the tray, writing `link` with any target table. This is the single change that answers the most user complaints in the file.

**Rule 2 governs this whole phase.** Counting is explicitly permitted, because the rule reads *record, organize, count*, and two users defended it unprompted. **A count of falls is a count. A direction, a delta, a trend or a verdict is not.** Build the count, refuse the arrow.

## Phase 6: the interface

`docs/findings/audit-chrome-motion.md` has the owner's three observations answered as one design each. Do those three first, in this order, because each is one component that the rest depend on: the single `PageBar`; `HeaderActions` into the `title` slot; `PageAction` published to `SectionEmpty`.

**Rule 12 applies to the four onboarding screens**, which gain a bar they were never drawn with: screenshot on a real device, `needs-design-review` issue, `DESIGN.md` 14, `HANDOFF.md`. All three, at the moment of building.

Then `audit-destinations.md` and `audit-sections.md`, high severity first. The dead pull-to-refresh on all 62 screens and the ripple drawing outside the card clip are the two a person meets most often.

## What not to do

- **Do not close the 13 issues labeled `deferred-by-d141`.** Rule 24, D180. Closing them reverses an owner decision.
- **Do not work the 28 issues in "Owner review, no code needed".** They wait on his eyes; no amount of work closes them.
- **Do not edit `ProjectDetailScreen.kt`, `CaptureSheet.kt`, or `PinnedGroup.kt`.** Frozen, D112 and D199.
- **Do not build a per-dose log, paired readings, foreign-key indexes, or delete `ui/components/Press.kt`.** All four are the owner's, listed at the end of `docs/FINDINGS.md`.
- **Do not reopen D67, D84, or D86.** The unencrypted-readable-copy question is closed and was closed after this exact argument was made.
- **Do not add reminders, notifications, cloud, sync, or an account.** Permanent design decisions, not gaps.
- **Do not let the app conclude anything.** No ranges, thresholds, trends, arrows, color by value, or "how they are doing".
- **Do not build the APK.** #395 is last and it is the owner's call when.

## Finish condition

Phases 1 through 4 complete and proven on the phone, phase 5's first five items done, phase 6's three chrome items done. `tools/verify.sh` no worse than the baseline you recorded at the start. `HANDOFF.md` rewritten to current truth, `DECISIONS.md` carrying a D number for every decision you made, and the board reflecting what changed.

**One report at the very end.** Lead with what a person can no longer lose.
