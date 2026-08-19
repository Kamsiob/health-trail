ORPHAN-COLUMN | attachment.entry_id | no | no | no | Paper can never hang on a call, visit or incident, so every photograph becomes a loose Document.
ORPHAN-COLUMN | attachment.bill_id | no | no | no | The itemized bill and the EOB can never sit on the charge they dispute.
ORPHAN-COLUMN | attachment.measurement_id | no | no | no | A photo-log or wound series is impossible.
ORPHAN-COLUMN | attachment.project_id | no | no | no | Project papers route through project_paper only.
ORPHAN-COLUMN | attachment.person_id | no | no | no | The owner's #370 ruling shipped as schema only.
ORPHAN-COLUMN | document.entry_id | YES (Repository.kt:4988 documentsOnIncident joins it) | no | yes | An incident's Documents section is structurally, permanently empty for every user forever.
ORPHAN-COLUMN | appointment.attended_* | YES (search, Repository.kt:4750 reads outcome_note) | no | no | An appointment can never be marked attended or missed.
ORPHAN-COLUMN | appointment.outcome_note | YES (search only) | no | no | The one line the next prep sheet most needs is unwritable.
ORPHAN-COLUMN | medication.prescriber_person_id | no | no | no | The drug and the doctor who ordered it never connect.
ORPHAN-COLUMN | measurement.reported_by_person_id | no | no | no | A family reading and a nurse reading are indistinguishable.
ORPHAN-COLUMN | standing_instruction.given_to_person_id | no | no | no | Hospice's whole job, who was told what, is schema with no app behind it.
ORPHAN-COLUMN | measurement.chapter_id | writer (recordMeasurement:1090) | YES | no | Written on every reading, shown on no chapter screen.
ORPHAN-COLUMN | appointment.chapter_id / organization_id | writer | partial | no | ChapterScreen lists incidents, milestones, documents, entries only; no OrganizationScreen.kt exists.
ORPHAN-COLUMN | cost_sheet.* / cost_entry.* | no | no | no | Two whole tables with no code path.
ORPHAN-COLUMN | link.source_table/target_table | YES (entriesAbout, projectsOnEntry) | entry->project and entry->about only | partial | A general-purpose join table wired for one direction of one entity type.
ORPHAN-COLUMN | ABOUT_HEADINGS["medication"] (Repository.kt:2047) | writer yes | no MemosAbout on MedicationScreen | half | A memo about a medication saves and is never shown back.
ORPHAN-COLUMN | device.label / origin_device | merge tiebreak only (Merge.kt:211) | no label writer | no | After a keep-both merge nothing says which phone wrote a row.

ROOT-CAUSE | The paper axis has exactly one hook | 19 findings | WIRING+SCREEN | createDocument (Repository.kt:3321) is the only attachment writer and only ever sets document_id; one image, PickVisualMedia.ImageOnly, no camera, no add-page, edit never touches the photo | Give createDocument/addAttachment the other five FK parameters and put one "Add a photo" control on incident, bill, measurement, person.
ROOT-CAUSE | Links are write-once at capture | 11 findings | WIRING | linkEntryToThread and linkEntryToProject fire only from the capture form and the tray's fileEntry; NoteTarget is only constructed from a thing's own screen | One shared "File this under..." action on EntryScreen and the tray writing into `link` with any target table.
ROOT-CAUSE | The person axis is schema without app | 9 findings | WIRING+SCREEN | Three person FKs have neither writer nor reader; PersonScreen shows only entries, appointments, memos | Care-team chip on the medication, measurement and instruction forms; three more sections on PersonScreen.
ROOT-CAUSE | An appointment has no "after" | 7 findings | WIRING | attended_* and outcome_note read by search, written by nothing; no missed state at all | Attended/missed control plus one note field, and print last time's outcome on the next prep sheet.
ROOT-CAUSE | A reading is one bare number | 10 findings | SCHEMA+WIRING | One value per measurement, categorical and event logs as free text, and recordMeasurement never puts the value on the entry it writes (Repository.kt:1095) | Put the number in the entry title today (fixes the month review immediately); the pair needs the owner.
ROOT-CAUSE | Handoff is a fixed menu of five | 11 findings | SCREEN | Incident, prep sheet, emergency card, calendar month, one photo, all NotebookShell.kt:1170-1267 | One "Share this" builder over the existing Readable renderer taking a filter instead of a fixed query.
ROOT-CAUSE | Entities that are foreign keys but not places | 10 findings | SCREEN | organization_id on appointments, bills, medications with no OrganizationScreen.kt; chapter and thread never gather appointments or measurements; cost_sheet has no screen | Build OrganizationScreen and add the two missing sections to ChapterScreen, over queries that already exist.
ROOT-CAUSE | Nothing repeats itself | 8 findings | SCREEN | No recurrence, no duplicate, no copy-last, no camera return, tray's "not now" unstored | A "Do this again" action reopening the capture form prefilled from the last row of the same kind.
ROOT-CAUSE | Retrieval surfaces are hard-coded lists | 6 findings | WIRING | Search runs over ten live_ tables; incidents, measurements and memos are not among them; the trail filters by kind and month only; Money browses by state only | Add the missing three run(...) lines and a person filter to the trail.
ROOT-CAUSE | Genuinely contested or rule-2 bounded | 14 findings | POLICY | Twelve CONTESTED pairs plus per-dose logging, counting behaviors, unencrypted export | Owner rules once each; several dissolve if the contested thing becomes a per-notebook choice.

ORDER
1. Retrieval surfaces (6 findings, ~10 lines of Kotlin)
2. A reading is one bare number, entry-title half only (~3 lines)
3. Appointment has no "after" (7 findings, one control and one field over existing columns)
4. The paper axis (19 findings, largest single block)
5. Links are write-once (11 findings, one reusable sheet)
6. The person axis (9 findings, three chips and three sections)
7. Entities that are not places (10 findings, one new screen plus two sections)
8. Handoff menu of five (11 findings, a real builder screen)
9. Nothing repeats (8 findings, touches every capture form)
10. Contested and rule-2 (14 findings, owner time, no code)
