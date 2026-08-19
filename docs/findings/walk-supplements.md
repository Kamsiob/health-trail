SUPPLEMENT, from the sub-walks. Detail the summary lines did not carry.

CORRECTIONS
Care-team name: POSSIBLE, 3 taps (PersonScreen.kt:155, write Repository.kt:584).
Entry date: POSSIBLE, 4 taps from trail (EntryScreen.kt:144, write Repository.kt:892). Rule 17 holds for entries only; medication_event is insert-only (Repository.kt:5960).
Wrong thread: IMPOSSIBLE. fileEntry only inserts and sets is_unfiled = 0 (Repository.kt:1491,1505), never reset to 1, no DELETE/UPDATE on entry_thread.
Medication never started: POSSIBLE, 3 taps, tombstone (Repository.kt:1186) distinct from stopped (:5926).
Duplicate people: IMPOSSIBLE. Merge.kt:31 is backup/restore only; entry_person unwritable after capture (Repository.kt:5390).

RELAPSE
Reopening an incident works, 1 tap, no confirm (IncidentScreen.kt:600-604).
The shadow entry reportIncident writes stores null title when blank (Repository.kt:4849) while the incident stores a placeholder (:4840), so the two disagree.
A care thread's ended_edtf/end_note are read at Repository.kt:4160,4181 and CareThreadsScreen.kt:99-101 with ZERO writers. end_note appears once in the whole repo.
aboutFor is LIMIT 1 (Repository.kt:6896), so a memo has one host by construction.
An incident cannot contain medications (entry has no medication_id) or appointments (appointment has no incident_id). IncidentDetail is entries + people + documents + violations only (Repository.kt:4929-4941).

APPEAL VIA PROJECTS
An existing document attaches in 4 taps (ChoosePaperScreen.kt:51-121, fillProjectPaper Repository.kt:7545), but no UI ever writes project_paper.direction, so sent-versus-received is read-only forever (ProjectPaperworkScreen.kt:62-64).
project_date.source_document_id / source_entry_id are never written (Repository.kt:7115-7131).
Rival second store for who-has-it: project.waiting_on (ProjectSetupScreen.kt:262-286) alongside project_standing.
project_step has no due column (schema.sql:1157-1186), so an overdue STEP cannot exist; an overdue project DATE does take the Projects lead (ProjectsScreen.kt:125-137).
next_up is appointments only (Repository.kt:7846-7853). No notification or alarm API anywhere in the app.
project.notes has no UI writer.
Standing changes are absent from the project trail (Repository.kt:7017-7065 = entries + reached stages + dates).
A logged call cannot be backdated (LogCallSheet.kt:56-57).

SECOND PERSON
Launch lands on Notebook, not Today (ui/ShellState.kt:60).
Add is 4 taps; switch is 3 taps.
Three unscoped queries: ownTemplates (Repository.kt:3090, custom_template has no subject_id, schema.sql:1249), organizationNamed (Repository.kt:2153, schema.sql:291), and the readable export mixes both people per year page with both names joined as owner (ReadableArchive.kt:234-260, ReadableRows.kt:112-117).
is_active has no unique index (schema.sql:210); makeSubjectActive (Repository.kt:236-247) is two un-transacted writes despite a comment claiming one transaction; activeSubject (:351) breaks a tie by ORDER BY created_at LIMIT 1, silently preferring the older person.
Capture targets the right subject (NotebookShell.kt:2981 -> :3044) but displays nothing: zero "subject" hits across CaptureFormScreen, CaptureBloom, CaptureSheet, Capture.
NO TEST exercises addSubject or makeSubjectActive.
