REFUTED | STROKE "no progress card on Today" | AddCardSheet.kt:359-366 builds a type="measure" offer for every tracked thing; TodayField.kt:2299 gives it the Progress hue.
REFUTED | REHAB "card gallery has no projects card" | AddCardSheet.kt:248 PROJECT_CARD_TYPES = project_standing, project_date, project_steps. The authorized-through date is exactly project_date.
REFUTED | "incidents are outside search" | Repository.kt:4845-4855 writes an entry kind="incident" carrying title and description; Repository.kt:4756 searches live_entry on title, body. The real defect is only that the hit is labeled under The trail, not Incidents.
REFUTED | "no plain readable copy" | DECISIONS.md:1863 (D84 confirming D67) is an owner decision made after this exact contradiction was surfaced; D86/rule 23 forbids reopening it.
REFUTED | "no dose log" | contract/schema.sql:531-533 states it in the schema comment: does not remind, does not alert, does not track doses taken. Deliberate scope.
REFUTED | "no camera intent" | AddDocumentScreen.kt:194-198: PickVisualMedia is used precisely because it asks for no permission; a camera or storage intent would grant access the app deliberately does not take.
REFUTED | "bill has no project_id" | contract/schema.sql:1326-1340 link(source_table, source_id, target_table, target_id, relation) is generic; bill-to-project is wiring, not schema.
REFUTED | "counting behaviors violates rule 2" | Rule 2 says "Record, organize, count." Readable.kt:202-213 already counts entries by kind while refusing a total. Counting nights is permitted; only a total-as-verdict is not.

CONFIRMED | attachment has exactly one writer | Repository.kt:3320-3328 is the only insertRow("attachment") and sets document_id only; schema.sql:817-823 declares entry_id, bill_id, project_id, measurement_id, person_id.
CONFIRMED | document.entry_id has no writer | Repository.kt:3305-3317 inserts no entry_id, yet Repository.kt:4994 joins live_entry e ON e.id = d.entry_id.
CONFIRMED | month review prints a reading with no number | Repository.kt:1101 titles the entry measureName(measureId), value goes only to measurement.value_number at :1112, Readable.kt:242-244 renders title and body only.
CONFIRMED | MedicationScreen never shows its memos | Repository.kt:2039-2048 ABOUT_HEADINGS includes "medication"; no MemosAbout in MedicationScreen.kt.
CONFIRMED | measure/measurement absent from ABOUT_HEADINGS | Repository.kt:2039-2048 lists nine tables, neither is one.
CONFIRMED | standing_instruction.given_to_person_id zero references | schema.sql:936 declares it; the string appears nowhere under main/kotlin.
CONFIRMED | medication.prescriber_person_id zero references | schema.sql:558.
CONFIRMED | origin_device on every row and on no screen | zero hits under ui/.
CONFIRMED | shared paper always .jpg / image/* | Share.kt:141 type="image/*", Share.kt:164 "$cleaned.jpg", regardless of stored mime_type.
CONFIRMED | a memo can never be attached after writing | NoteTarget constructed at eleven call sites, all at the moment of writing; nothing re-targets an existing memo.
CONFIRMED | chapter dates are uncorrectable | Repository.kt:383 renameChapter is the only chapter mutator besides moveToChapter at :3873, which stamps the current date.
CONFIRMED | no OrganizationScreen, no cost sheet screen | no matching file in ui/screens/, though cost_sheet and cost_entry are in the schema.

CLUSTER | The paper never reaches the thing it is about | twelve findings across six personas | Repository.kt:3305-3328 is the only attachment/document writer and fills document_id alone.
CLUSTER | Nothing records that an appointment happened | six findings | attended_* and outcome_note read by Repository.kt:4750 and written by nothing; already logged as DECISIONS.md:3039 / #371 item 1.
CLUSTER | Filing is one-shot, at creation only | five findings | linkEntryToThread/linkEntryToProject fire only from NotebookShell.kt:3067,3078 and fileEntry at Repository.kt:1491, whose only destination parameter is threadId.
CLUSTER | Handing over a span of time | seven findings | the share surface is a fixed list; no arbitrary date range, no per-section share.

SCHEMA | paired readings | measurement.value_number is a single column; a pair needs a second numeric column or a companion row convention.
SCHEMA | a missed appointment | appointment has attended_* but no negative state; "did not happen" needs a column, not a null.
SCHEMA | who broke an instruction | instruction_violation has no person column.
SCHEMA | repeated acknowledgment | standing_instruction.acknowledged_* are single-valued; a rotating team needs a child table.
SCHEMA | belongings | no table for an object that recurs across incidents.

RULE-2 | centile lines and a "how is he doing" summary | interpretation of a measurement, forbidden outright.
RULE-2 | comfort score | a judgment on a person's state; the correct form is a dated note.
RULE-2 | a measure card showing "the week's change" | a card may show readings, never a change or a direction; build the card, refuse the delta.
