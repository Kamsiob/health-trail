RESOLVED | Free capture vs prefilled repeat fields | Both; prefill is per-kind memory | Rule 23 with rule 13: an editable prefill from the last row of the same kind costs the hospital nothing and saves dialysis twelve typings.
RESOLVED | Thread gathers appointments and measurements | Stroke wins, as separate groups | Rule 18 says links go both ways; the drowning objection is solved by rules 15 and 22 with a quiet grouped section, not by refusing the link.
RESOLVED | Project deadline on Today | Dissolved, already exists | AddCardSheet.kt:248 has project_date and the gallery is rearrangeable per person.
RESOLVED | Whole archive vs per-subject export | Both; archive stays the backup | Rule 23's privacy filter is not a tiebreaker: handing one parent a file containing a second person's notebook fails it outright.
RESOLVED | Emergency card as Today's lead | Dissolved, per-notebook choice | The lead slot is already a rearrangeable gallery and preference storage exists.
RESOLVED | Notes attachable to a tracked thing | Child wins | Rule 2 forbids the app concluding, not the person writing; measure and measurement join ABOUT_HEADINGS.
RESOLVED | Chapter dates editable and precise | Rehab wins, at rule 17's precision | Unknown is first-class, so the field accepts "sometime in April" or nothing.
RESOLVED | Counting event-log tracked things | Memory care wins | Rule 2 literally reads "record, organize, count"; only a total as a verdict, a delta or a direction stays forbidden.
RESOLVED | Bills bound to documents and projects | Both, at different moments | The link exists; the 10pm objection is removed by making filing never happen at capture.
RESOLVED | Device named on every row | Dementia wins for the default | Rule 20: origin_device belongs in the merge log and on rows a merge touched, never as a column of noise.
RESOLVED | Unencrypted readable copy | Refused; the share builder answers it | D84/D67 closed and D86 forbids reopening, but a per-section arbitrary-range readable through the share sheet meets the real need.
OWNER | Should a family per-dose log exist (given, refused, missed), when contract/schema.sql:531-533 states the app deliberately does not track doses?

REFUTATION-STANDS | progress card; projects card; D84 readable copy; dose log; bill project_id; counting behaviors.
REFUTATION-OVERREACHED | incidents are searchable | Only CREATION mirrors an incident: updateIncident (Repository.kt:5104) and resolveIncident (:5154) never touch the mirror entry, so a corrected incident is findable only by its original words, the trail shows the stale title, resolution_note is unsearchable anywhere, measurement.value_text never reaches an entry so categorical values are unsearchable, and the hit opens the mirror entry (NotebookShell.kt:1394) rather than the incident.
REFUTATION-OVERREACHED | no camera intent | Permission-free picking is right, but the refutation answered "camera" and killed "a PDF cannot enter": ACTION_OPEN_DOCUMENT needs no permission either, so ImageOnly (AddDocumentScreen.kt:434) is a choice, not a constraint.

CLOSING-LIST
1 | Reading's value never on its entry | WIRING | 11 settings | recordMeasurement (Repository.kt:1095) titles the entry with the measure name only, so a shared month prints "Weight" with no number | Put value and unit in the entry title, value_text in the body.
2 | Retrieval surfaces are hard-coded | WIRING | 12 | Search runs ten tables; incidents, measurements and their edits are not among them; the trail filters by kind and month but not person | Add the missing run(...) lines plus a person filter, and re-stamp the mirror entry on incident update and resolve.
3 | An appointment has no "after" | WIRING+SCHEMA | 12 | attended_* and outcome_note read by search, written by nothing; "did not happen" has no column | Attended/missed control and one outcome field, printed on the next prep sheet; the missed column needs owner sign-off.
4 | document.entry_id has no writer | WIRING | 11 | documentsOnIncident (Repository.kt:4988) joins a column nothing writes, so an incident's Documents section is empty for everyone forever | Pass an entry id into createDocument and set it from the incident's add control.
5 | The paper axis has one hook | WIRING+SCREEN | 13 | Repository.kt:3320-3328 is the only attachment writer and sets document_id alone | Give addAttachment the five FK parameters and one "Add a photo" control on incident, bill, measurement, person.
6 | Filing is write-once at capture | WIRING | 12 | linkEntryToThread and linkEntryToProject fire only from the capture form; the tray files into care threads only | One "File this under" sheet on EntryScreen and in the tray, writing link with any target table.
7 | Memos are write-once and half-shown | WIRING+SCREEN | 10 | NoteTarget is built at eleven creation sites and never re-targeted; MedicationScreen has no MemosAbout despite ABOUT_HEADINGS carrying medication | Re-target action, the missing MemosAbout block, measure/measurement into ABOUT_HEADINGS.
8 | Chapter dates uncorrectable | WIRING | 7 | moveToChapter (Repository.kt:3873) stamps today; renameChapter is the only other mutator | Editable EDTF start and end, unknown allowed.
9 | Handoff is a fixed menu of five | SCREEN | 12 | NotebookShell.kt:1170-1267 | One share builder over the existing Readable renderer taking a date range and section filter.
10 | The person axis is schema only | WIRING+SCREEN | 9 | prescriber_person_id, reported_by_person_id, given_to_person_id have neither writer nor reader | Care-team chip on three forms, three sections on PersonScreen.
11 | Nothing repeats itself | SCREEN | 9 | No recurrence, duplicate or copy-last anywhere | A "Do this again" action reopening the form prefilled from the last row of that kind.
12 | Foreign keys that are not places | SCREEN | 8 | organization_id on appointments, bills, medications with no OrganizationScreen; ChapterScreen never shows appointments or measurements | Build OrganizationScreen, add two sections to ChapterScreen.
13 | One image per document, images only | WIRING+SCREEN | 8 | ImageOnly refuses PDFs; createDocument writes at most one attachment | Add OpenDocument alongside the media picker; let a document hold several attachments.
14 | Editing a document cannot add a photo | SCREEN | 6 | NotebookShell.kt:2314 never touches the image | Let the edit form set an attachment when none exists.
15 | Shared paper is always .jpg | WIRING | 5 | Share.kt:141,164 hardcode image/* and .jpg | Use the stored mime type and extension.
16 | Paired readings | SCHEMA | 7 | measurement.value_number is one column | Owner call: second numeric column or companion-row convention.
17 | Instruction acknowledgment and breach | SCHEMA | 4 | acknowledged_* single-valued, instruction_violation has no person column | Child table for acknowledgments, person column on the violation.
18 | Per-subject export | POLICY+WIRING | 4 | The archive is the whole database | A subject-scoped readable export beside the whole-notebook archive.
