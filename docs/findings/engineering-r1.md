HOSPITAL | critical | missing-link | Photographed the after-visit summary to hang on today's appointment; `document` files only to a chapter, entry or project, and the prep sheet shows questions, changes and memos but never papers.
HOSPITAL | high | cannot-file | Tried to add a photo of the bruise to an incident report; the picker exists only on Save a document, though `attachment` already carries `entry_id`, `bill_id`, `measurement_id` and `person_id`.
HOSPITAL | high | dead-end | Rounds happen every morning; there is no recurring appointment, so each day is a hand-typed appointment and yesterday's unasked questions do not move to today's sheet.
HOSPITAL | high | cannot-retrieve | Asked to hand the night nurse dad's current medication list; only the emergency card, one incident writeup, one prep sheet and one month can be shared.
HOSPITAL | high | missing-link | Opened the ward chapter expecting the week's appointments and recorded numbers; ChapterScreen lists incidents, milestones, documents and entries only, though both tables carry `chapter_id`.
HOSPITAL | medium | wrong-place | Missed the neurology round and wanted it marked not attended; `appointment` has attended dates and an outcome note but no missed state, so it reads as unfilled.
HOSPITAL | medium | cannot-retrieve | Wanted every entry naming the pulmonologist across two chapters; the trail filters by kind only, not by person, thread or chapter.
REHAB | critical | wrong-place | Kept the authorized-through date as a project date; Today's card gallery offers thirteen types and none is projects, so the date that ends the stay lives two taps away.
REHAB | high | missing-link | Filed the appeal project and the denied claim separately; `bill` links to an organization and a chapter but has no `project_id`.
REHAB | high | cannot-file | Therapy goals came as minutes per discipline per day; a tracked thing holds one number, so PT, OT and speech become three measures or one note.
REHAB | high | missing-link | Filed the authorization letter as a project paper and then wanted it on the discharge chapter too; a document holds one chapter and one project filing, not both plus a thread.
REHAB | medium | cannot-retrieve | Wanted to hand the case manager the therapy record; the only date-range share is one calendar month, so a three-week authorization period cannot be handed over.
REHAB | medium | tap-cost | Recording that the therapist said something takes the gold button, Log a visit, then picking the person and the thread again from inside the thread you were already reading.
STROKE | critical | missing-link | Opened the physical therapy thread expecting its appointments; `entry_thread` links entries only, so appointments and measurements can never join a thread running across three settings.
STROKE | high | missing-link | Moved him from inpatient to outpatient and wanted the walking distance series to continue across chapters; a measure shows readings but no chapter divisions (unverified beyond MeasureScreen).
STROKE | high | cannot-file | Wanted the weekly video of his gait, or even a photo series; no audio and no attachment path on a measurement, so a photo-log measure is dated free text.
STROKE | high | cannot-retrieve | Asked to show the new speech therapist what the last one had been doing; nothing exports a single thread, only the whole encrypted archive.
STROKE | medium | dead-end | Wanted a tracked thing on Today so the week's change is the first thing seen; the card gallery has no progress card.
STROKE | medium | missing-link | `measurement.reported_by_person_id` exists, but the person's own page assembles entries and appointments, not the readings they gave.
DIALYSIS | critical | cannot-file | Pre and post session weight is one pair per run; the form has one number, so half the reading goes in the note and cannot be counted.
DIALYSIS | high | tap-cost | Three sessions a week is thirteen appointments a month typed one at a time with no repeat, no duplicate and no copy-last.
DIALYSIS | high | dead-end | Skipped Tuesday because the ride never came; there is no missed state on an appointment, so the one thing the unit will ask about becomes a memo.
DIALYSIS | medium | cannot-file | `cost_sheet` and `cost_entry` are in the schema with no screen anywhere, so every ride has to be a bill.
DIALYSIS | medium | missing-link | The dialysis unit is an organization on the appointment, but no organization page gathers its appointments, bills and people (no OrganizationScreen.kt exists).
DIALYSIS | medium | wrong-place | Standing instruction about the fistula arm belongs on every session; instructions are their own section with no link to appointments or chapters.
DIALYSIS | low | cannot-retrieve | Wanted the last twelve sessions' numbers on one page for the nephrologist; sharing a measure's history is not offered.
CONTESTED | Hospital wants unfiled capture nearly free and file later; dialysis wants the same three fields prefilled every session and would rather the app repeat itself than ask.
CONTESTED | Stroke wants a thread to gather appointments and measurements as its spine; rehab says the thread should stay entries only, because the therapy record is what people read and a schedule would drown it.
CONTESTED | Rehab wants a project deadline on Today; hospital says Today should only hold what happens today, and a date six weeks out belongs in Projects.
CANCER | critical | cannot-file | Photographed the itemized bill and the EOB, opened the bill in Money, no attachment control at all: `attachment.bill_id` has no writer anywhere in Repository.kt (only `document_id` is inserted, line 3321) and BillScreen.kt never mentions documents.
CANCER | critical | dead-end | Saved "Discharge summary, blue folder" with no picture, photographed it that evening, tapped edit: correcting a document never touches the photograph by design (NotebookShell.kt:2314), so a document saved without an image can never gain one.
CANCER | high | cannot-file | The picker is `PickVisualMedia.ImageOnly` (AddDocumentScreen.kt:434) and a document joins exactly one attachment, so PDFs are refused and each page becomes its own document.
CANCER | high | missing-link | `createDocument` takes no entry id, so `document.entry_id` is never written and `documentsOnIncident` can return nothing from real use.
CANCER | high | wrong-place | Filed a hallway call into the wrong care thread and could not move it: `linkEntryToThread` is called only from the capture form and the tray's one-shot `fileEntry`; EntryScreen offers no thread action.
CANCER | high | missing-link | Logged a call from Today, realized days later it belonged to the appeal, no way to say so: `linkEntryToProject` fires only at creation time.
CANCER | medium | dead-end | Wanted to mark an infusion attended and write what was said: `attended_*` and `outcome_note` exist in the schema with no writer and no field.
CANCER | medium | cannot-retrieve | A shared paper always leaves as `<title>.jpg` typed `image/*` (Share.kt:140,162), whatever was stored.
CHILD | critical | cannot-retrieve | The archive is the whole database, every subject, no per-person export, so sharing the child also shares the grandmother's notebook.
CHILD | high | cannot-file | `attachment.person_id` (the owner's #370 ruling) has no writer, so a school nurse's card photo can only become a loose Document.
CHILD | high | missing-link | PersonScreen shows entries, appointments and memos only, though `question.person_id`, `medication.prescriber_person_id` and `standing_instruction.given_to_person_id` all exist.
CHILD | high | cannot-file | `attachment.measurement_id` has no writer and a measurement has no document link, so the clinic letter the height came from cannot be kept with the reading.
CHILD | medium | wrong-place | Notes attach to appointment, person, document, bill, incident and project only (NotebookOverlays.kt call sites), not to a medication or a tracked thing.
CHILD | medium | cannot-file | A multi-page IEP cannot be one document, the one-image reason again.
CHILD | medium | tap-cost | The pull was toward centile lines and a "how is he doing" summary, and that temptation is itself the finding: rule 2 forbids it and it must stay forbidden.
CHILD | low | missing-link | After a keep-both merge there is no visible answer to which parent wrote a row (unverified whether any screen surfaces `origin_device`).
HOSPICE | critical | missing-critical | The whole job is who was told what, and `standing_instruction.given_to_person_id` has no writer anywhere: an instruction records its wording but never who it was given to.
HOSPICE | high | dead-end | The instruction holds one `acknowledged_edtf` and one `acknowledged_how`, so a second acknowledgment overwrites the first and a rotating team cannot be recorded.
HOSPICE | high | missing-link | The emergency card holds `resuscitation_document_location` as free text with no link to the photographed POLST three taps away.
HOSPICE | high | cannot-file | No link writer exists for instructions, so the signed wishes and the words that quote them live in separate sections forever.
HOSPICE | medium | cannot-retrieve | Only incidents, the emergency card, the prep sheet and the month review have a share (NotebookShell.kt:1170-1267); the standing instructions list does not.
HOSPICE | medium | dead-end | No attended or outcome field, so "she came Thursday and said this" is re-entered as a separate call or memo.
HOSPICE | medium | missing-link | No instruction section on a person, the other half of the given-to gap.
HOSPICE | low | wrong-place | Any comfort score is rule 2 interpretation; the correct answer is a dated note in the trail, so this stays out.
CONTESTED | Cancer wants the archive to stay one whole file because an appeal needs everything; child wants a per-subject export because handing the other parent the file hands them a second person's notebook.
CONTESTED | Hospice wants the emergency card pinned as Today's lead permanently; cancer wants the next appointment there, and there is exactly one lead.
CONTESTED | Child wants notes attachable to a tracked thing so a weight carries its context; hospice argues any text beside a number starts being read as a verdict on it.
NURSING HOME | critical | cannot-file | Photographed a bruise after a fall and saved it as a document, but the incident's own "Documents" section stayed empty forever: nothing in the app ever writes `document.entry_id`, which is the only join `documentsOnIncident` reads.
NURSING HOME | high | wrong-place | Tapped "Add" on an open incident to write what the day nurse said face to face and got a Log a call form, because `onAdd` hardcodes `CaptureKind.CALL` with no way to switch kind.
NURSING HOME | high | tap-cost | The care plan meeting is quarterly, appointments have no repeat of any kind, and the new prep sheet does not show the last meeting's `outcome_note`, so she retypes the standing agenda four times a year.
NURSING HOME | high | cannot-file | The care plan is six pages; a document holds exactly one photograph (`createDocument` writes at most one attachment, no add-page anywhere), so it becomes six unrelated documents.
NURSING HOME | medium | missing-link | Wanted the October fall raised at the meeting: a question can point at an entry or a medication but never at an incident, so the incident cannot reach the prep sheet.
NURSING HOME | medium | dead-end | A note about the night shift sat in "waiting to be filed"; the tray files only into care threads (`fileEntry(entryId, threadId)`), never into the open incident it obviously belongs to.
NURSING HOME | medium | cannot-retrieve | Wanted every fall since admission as one document for the meeting; share offers one incident, one prep sheet, one calendar month, or the emergency card, and nothing spanning a stay.
NURSING HOME | low | missing-link | A person's screen shows their calls, visits and appointments, but an incident's people list does not show that person's other incidents.
REHAB | critical | dead-end | Recorded the move from hospital to rehab three days late; `moveToChapter` stamps today, and no screen can correct a chapter's start or end (only `renameChapter` exists), so the admission date is permanently wrong.
REHAB | critical | cannot-file | The discharge summary arrived as a PDF by email; the document picker is `PickVisualMedia.ImageOnly`, so a PDF cannot enter the notebook at all.
REHAB | high | tap-cost | There is no in-app camera either, so filing a paper handed over at the desk means leaving the app, using the camera, returning, and re-entering capture.
REHAB | high | missing-link | The discharge appointment has no documents on it: an appointment links to a person, an organization and a chapter, never to the paper it produced.
REHAB | medium | cannot-file | PT and OT minutes need two separate tracked things because a reading holds one number, and the same happens to blood pressure.
REHAB | medium | missing-link | The rehab bill cannot be attached to the appeal project; the generic `link` table exists but is wired only for entry-to-project.
REHAB | medium | cannot-retrieve | Wanted one "where he is up to" page for the discharge planner; the writeup is per appointment and the review is a calendar month, not the stay.
ASSISTED LIVING | critical | cannot-file | Photographed the monthly statement expecting it on the bill; `attachment.bill_id` is in the schema with no writer, and the bill form takes only what, amount, state and notes.
ASSISTED LIVING | high | missing-link | Opened the bill to find the letter disputing it: the bill screen shows chapter, violations and memos, never documents, so the paper and the charge never meet.
ASSISTED LIVING | high | cannot-retrieve | Searched for a charge by amount and could only browse Money by state; there is no filter by organization or by month on the bill list.
ASSISTED LIVING | medium | missing-link | The pharmacy is an organization on a medication, but there is no screen for an organization that gathers its bills, people and appointments.
ASSISTED LIVING | medium | tap-cost | Called the med tech from the person's screen, the dialer opened, and coming back offered no "log that call" with the person already filled in.
ASSISTED LIVING | low | cannot-file | A document's folder is free text with no way to rename a folder across the documents already in it.
MEMORY CARE | critical | cannot-file | A photo log tracked thing cannot hold a photo: `attachment.measurement_id` is in the schema with no writer, so the bruise picture goes to Documents and never reaches the series.
MEMORY CARE | high | missing-critical | Nothing holds belongings, so the fourth disappearance of the same blue cardigan is a fourth unrelated incident with no way to say it is the same object.
MEMORY CARE | high | cannot-retrieve | Sundowning is an event-log measure stored as free text, so she can read the notes but the app cannot even count the nights, which is all she wanted.
MEMORY CARE | medium | missing-link | Behavior after a medication change: the medication screen shows its own events and questions, but a tracked series cannot be opened from the medication that prompted it.
MEMORY CARE | medium | dead-end | Filed the photo of the missing hearing aid as a document, and the incident about it can never show that photo, same `document.entry_id` gap.
MEMORY CARE | medium | tap-cost | Logging the same short behavior note nightly is a full staged form each time with no way to repeat the previous one.
MEMORY CARE | low | missing-link | A standing instruction records who it was given to, but that person's screen does not list the instructions they were given.
CONTESTED | Rehab wants chapter dates editable and stamped precisely; nursing home says a long stay has no meaningful move date and would rather the app never asked.
CONTESTED | Memory care wants event-log tracked things countable; assisted living says counting behaviors is one step from implying a trend, and would keep them plain notes.
CONTESTED | Assisted living wants bills bound to documents and projects as first-class links; nursing home says more filing destinations makes every capture a decision she does not want at 10pm.
HOME SIBLINGS | critical | cannot-retrieve | Shared the month to her brothers and every reading printed as "Weight" with no number: `Readable.monthReview` renders an entry's title and body and `recordMeasurement` never puts the value on the entry.
HOME SIBLINGS | high | missing-critical | After a keep-both restore nothing says which phone wrote which entry; `origin_device` is on every row, surfaced on no screen, and `device.label` has no writer.
HOME SIBLINGS | high | cannot-file | Wrote a memo from the Memos tab, then wanted it on Tuesday's appointment; `NoteTarget` is only set from the thing's own screen at the moment of writing, so a memo can never be attached afterward.
HOME SIBLINGS | high | missing-link | Photographed the bruise as a document and expected it on the fall incident; `documentsOnIncident` joins `document.entry_id`, which nothing ever writes, so that section is permanently empty.
HOME SIBLINGS | medium | cannot-retrieve | Searched "fall" and the incident did not appear; search runs over ten sections and incidents is not one of them.
HOME SIBLINGS | medium | dead-end | No way to mark an appointment attended or say what came of it; `attended_*` and `outcome_note` are searched but have no writer.
HOME SIBLINGS | medium | cannot-file | The unfiled tray's only destination is a care thread, so a stray capture belonging to a project, chapter or incident has nowhere to go.
HOME SIBLINGS | low | cannot-file | One photo per document, so a six-page discharge summary becomes six separate documents.
HOME HEALTH | critical | cannot-retrieve | Wanted a plain readable copy for the case manager; the readable HTML sits inside the encrypted archive (D67/D84), so the only handoffs are incident, prep, emergency card, month review, one photo.
HOME HEALTH | high | missing-link | Typed a rotating aide's name into "who" on a visit; with no care-team chip it stays free text and no person row is made, so "everything Maria did" cannot be pulled up.
HOME HEALTH | high | missing-link | A standing instruction has one `given_to_person_id` and `instruction_violation` has no person column, so with fourteen aides the instruction names one recipient and a breach names nobody.
HOME HEALTH | high | missing-link | The nurse's screen shows entries, appointments and memos but not the open questions waiting on her, though `question.person_id` already drives the prep sheet.
HOME HEALTH | medium | cannot-file | `medication.prescriber_person_id` exists and nothing writes it, so the drug and the doctor who ordered it never connect.
HOME HEALTH | medium | cannot-file | An agency letter can fill a project paper slot and nothing else; a document cannot be attached to an appointment, a medication or a person.
HOME HEALTH | medium | tap-cost | Every photo comes from the gallery picker, no camera intent, so a paper the nurse hands over is camera app, back, then pick.
HOME HEALTH | low | cannot-retrieve | The trail filters by kind, month and text but not by person.
DEMENTIA HOME | critical | missing-critical | No way to record a dose given, refused or missed; `medication_event` kinds are started/stopped/dose_changed/held/resumed/noted and the schema says plainly it does not track doses.
DEMENTIA HOME | high | cannot-file | A behavior log is a categorical tracked thing stored as free text at one value per reading, so months of sundowning can be reread but never counted.
DEMENTIA HOME | high | missing-link | A memo about a medication saves (`ABOUT_HEADINGS` includes medication) but `MedicationScreen` has no `MemosAbout` block, so it is written and never shown back.
DEMENTIA HOME | high | cannot-retrieve | Asked "how many times last month"; incidents are outside search and the month review counts entries by kind only.
DEMENTIA HOME | medium | dead-end | Skipped the situation template, so there are no care threads, and the unfiled tray then has literally nowhere to file anything.
DEMENTIA HOME | medium | missing-link | `measure`/`measurement` are absent from `ABOUT_HEADINGS`, so a note explaining an odd reading has nowhere to attach but the reading's own note field.
DEMENTIA HOME | medium | cannot-retrieve | The month document has no medications section, so a month of dose changes leaves no trace in the handoff.
DEMENTIA HOME | low | tap-cost | The tray's "not now" is deliberately unstored, so the same items are stepped through again next visit.
CONTESTED | Dementia wants a per-dose log; home health says a family dose log would contradict the aide's own MAR and the app should hold only what the nurse said, in her words.
CONTESTED | Siblings wants the writing device named on every row after a merge; dementia is the sole writer and says a "who wrote this" line is a column of noise on every entry.
CONTESTED | Home health wants a plain unencrypted readable copy to hand a case manager; siblings, whose export lands in a synced Downloads folder, says the encryption is why she trusts it.
