-- Health Trail canonical schema.
--
-- This file is the contract. The Android app copies it into assets at build
-- time and executes it. The web scaffold loads the same file into SQLite
-- compiled to WebAssembly. Neither platform keeps a second copy, and neither
-- declares a table in its own source. If the build cannot read this file it
-- fails loudly rather than falling back to something stale.
--
-- Nothing here may be revised without an explicit decision from the owner,
-- because changing the schema after real data exists means discarding
-- someone's records. See contract/DATA-CONTRACT.md.
--
--
-- THE RULES THIS FILE EXISTS TO ENFORCE
--
-- 1. Every user data table carries the same six columns:
--
--      id            TEXT     a locally generated, time ordered, collision safe
--                             id. Never an integer. Never AUTOINCREMENT. Never
--                             reused. The only thing a foreign key points at.
--      created_at    INTEGER  milliseconds since epoch, UTC. Set once.
--      updated_at    INTEGER  milliseconds since epoch, UTC. Touched on every
--                             write. Also the conflict comparison key.
--      deleted_at    INTEGER  NULL means live. A timestamp means deleted.
--      origin_device TEXT     the device that created the row.
--      rev           INTEGER  increments on every local write to the row.
--
--    Two devices both creating row 47 has no correct merge, and fixing it
--    after real data exists means reassigning every id and every foreign key
--    on someone's records. That is why there is no AUTOINCREMENT below except
--    on change_log.seq and conflict_log.seq, which are local only and are
--    never synced.
--
-- 2. Deletion is always a tombstone. Set deleted_at, bump rev. Never issue a
--    DELETE against a user data table. If a row is simply gone there is
--    nothing left to tell a peer it was deleted, so the peer resurrects it on
--    the next sync and the deletion appears to undo itself, forever.
--
--    Two exceptions, both explicit. The full data wipe genuinely removes
--    everything including tombstones, which is the point of it. The tombstone
--    purge may remove tombstones older than the retention window below, and
--    only after every known paired device has acknowledged them. Until direct
--    sync exists there are no peers, so nothing is ever purged and that code
--    path is dead.
--
-- 3. TOMBSTONE RETENTION WINDOW: 730 days.
--
--    Chosen deliberately long. This app is used for years and lapse tolerance
--    is a stated value: someone stops for months and comes back, and a second
--    device may sit unopened for longer than that. A tombstone row is a
--    handful of bytes, so the cost of keeping it is nothing next to the cost
--    of purging one a dormant peer had not yet seen, which resurrects deleted
--    records. See DECISIONS.md.
--
-- 4. Every write appends to change_log in the same transaction as the write.
--    This is enforced here by triggers rather than by the application layer,
--    because a rule the application has to remember is a rule that gets
--    forgotten exactly once, silently. A trigger cannot be forgotten and it
--    cannot run in a different transaction than the statement that fired it.
--
-- 5. Every query filters tombstones. Enforced by the live_* views at the
--    bottom of this file plus the repository layer, not by discipline. One
--    forgotten "deleted_at IS NULL" is a data leak of something the person
--    believed they had deleted.
--
-- 6. Attachments are content addressed. A file is named by the hash of its
--    bytes. An attachment therefore can never conflict, transferring one twice
--    is free, and a corrupt transfer is detectable by rehashing.
--
--
-- CONVENTIONS
--
-- Times are INTEGER milliseconds since the Unix epoch, UTC, always. Local time
-- is a rendering concern and never a storage concern, because the device
-- timezone changes and the record must not.
--
-- Booleans are INTEGER 0 or 1, since SQLite has no boolean type.
--
-- Text that a person typed is stored exactly as typed. The app never
-- normalizes, trims meaning out of, or reformats what someone wrote down.

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;


-- ---------------------------------------------------------------------------
-- LOCAL TABLES
--
-- These describe this installation rather than the person's records. They are
-- not user data tables, they carry no tombstone, and they are the only place
-- AUTOINCREMENT appears.
-- ---------------------------------------------------------------------------

-- Key and value settings for this install. Holds device_id, the schema
-- version, the disclaimer acceptance timestamp, the last opened timestamp that
-- the digest reads, and the backup state.
CREATE TABLE IF NOT EXISTS app_meta (
  key        TEXT    NOT NULL PRIMARY KEY,
  value      TEXT,
  updated_at INTEGER NOT NULL
);

-- Devices this install knows about. Row one is always this device. Others
-- appear only when the person deliberately pairs one, which is not a v1
-- feature. Nothing here is ever transmitted anywhere on its own.
CREATE TABLE IF NOT EXISTS device (
  id          TEXT    NOT NULL PRIMARY KEY,
  label       TEXT,
  is_self     INTEGER NOT NULL DEFAULT 0 CHECK (is_self IN (0, 1)),
  created_at  INTEGER NOT NULL,
  -- The highest change_log.seq received from this peer. Meaningless for self.
  last_seq_in INTEGER NOT NULL DEFAULT 0
);

-- The append only change log. One row per write, in the same transaction as
-- the write, appended by the triggers at the bottom of this file.
--
-- This is the only way a peer can ask the one useful question, which is "give
-- me everything that changed after sequence N". Without it, sync means diffing
-- whole tables on every connection, which is slow, fragile, and cannot tell an
-- edit from a delete then recreate.
--
-- It is also useful immediately, well before any sync exists: it is what tells
-- the Today digest what changed since the person was last here.
--
-- seq is local only. It is meaningful solely on the device that wrote it, and
-- an importing device renumbers rather than trusting a foreign sequence.
CREATE TABLE IF NOT EXISTS change_log (
  seq        INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  table_name TEXT    NOT NULL,
  row_id     TEXT    NOT NULL,
  op         TEXT    NOT NULL CHECK (op IN ('insert', 'update', 'delete')),
  rev        INTEGER NOT NULL,
  changed_at INTEGER NOT NULL,
  device_id  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_change_log_seq_device ON change_log (device_id, seq);
CREATE INDEX IF NOT EXISTS ix_change_log_row        ON change_log (table_name, row_id);
CREATE INDEX IF NOT EXISTS ix_change_log_changed_at ON change_log (changed_at);

-- When two versions of a row meet, the newer updated_at wins, with
-- origin_device as a deterministic tiebreaker. The version that lost is
-- written here intact, with both sides readable, and the person is told
-- plainly that two versions existed and what was replaced.
--
-- A record keeping app that quietly eats an entry has failed at its one job,
-- which is why nothing is ever silently discarded.
CREATE TABLE IF NOT EXISTS conflict_log (
  seq          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  table_name   TEXT    NOT NULL,
  row_id       TEXT    NOT NULL,
  resolved_at  INTEGER NOT NULL,
  -- Which side won, for the plain explanation shown to the person.
  winner       TEXT    NOT NULL CHECK (winner IN ('local', 'incoming')),
  reason       TEXT    NOT NULL,
  -- Both versions as JSON, complete, so nothing is lost and either can be
  -- restored by hand if the person disagrees with the resolution.
  local_json   TEXT    NOT NULL,
  incoming_json TEXT   NOT NULL,
  -- Set when the person has seen the notice. Not a tombstone: this table is
  -- local and is not user authored content.
  seen_at      INTEGER
);

CREATE INDEX IF NOT EXISTS ix_conflict_log_unseen ON conflict_log (seen_at) WHERE seen_at IS NULL;

-- Applied migrations, so an upgrade never has to guess what state it is in.
-- Uninstalling to work around a migration is never allowed, so this has to be
-- right.
CREATE TABLE IF NOT EXISTS schema_migration (
  version    INTEGER NOT NULL PRIMARY KEY,
  applied_at INTEGER NOT NULL,
  note       TEXT
);


-- ---------------------------------------------------------------------------
-- USER DATA TABLES
--
-- Every one of these carries the six required columns. No exceptions, and no
-- AUTOINCREMENT anywhere below.
-- ---------------------------------------------------------------------------

-- The person being looked after. Usually one. The More tab offers a switcher
-- because someone may end up as the point person for two parents at once, and
-- discovering that after the schema is fixed would be expensive.
--
-- This is deliberately not a user account. There is no account anywhere in
-- this app.
CREATE TABLE IF NOT EXISTS subject (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  display_name  TEXT    NOT NULL,
  -- What the person calls them, which is often not the legal name.
  relationship  TEXT,
  date_of_birth TEXT,
  -- Which situation template configured this notebook. Kept so the app can
  -- tell what was applied without implying it may reapply and clobber edits.
  situation_template_id TEXT,
  notes         TEXT,
  is_active     INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
  -- A trail can be closed without being deleted. The record stays readable
  -- and exportable afterward.
  closed_at     INTEGER,
  closed_note   TEXT
);

-- A place and a period. Home, a hospital stay, a rehab facility, a nursing
-- home. Chapters answer "where", and they are drawn as stops on the trail.
CREATE TABLE IF NOT EXISTS chapter (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  name          TEXT    NOT NULL,
  kind          TEXT,
  organization_id TEXT REFERENCES organization (id),
  started_at    INTEGER,
  ended_at      INTEGER,
  -- Why the stay began, in the person's own words.
  reason        TEXT,
  -- Where they came from, so a transfer reads as a transfer rather than as two
  -- unrelated chapters.
  transferred_from_chapter_id TEXT REFERENCES chapter (id),
  transfer_note TEXT,
  notes         TEXT,
  sort_index    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_chapter_subject ON chapter (subject_id, started_at);

-- Parallel streams that run at the same time: physical therapy, occupational
-- therapy, speech, nursing, wound care. Threads answer "what is ongoing".
--
-- A thread can end while the notebook continues, and an ended thread keeps its
-- whole story. Its route renders at reduced opacity and keeps its color, so it
-- reads as finished rather than deleted.
CREATE TABLE IF NOT EXISTS care_thread (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  label         TEXT    NOT NULL,
  -- The template thread id this came from, where it came from one.
  template_id   TEXT,
  -- Index into the theme's thread route colors rather than a hex value, so the
  -- dark theme substitution happens in the theme and a stored color can never
  -- fail contrast.
  color_index   INTEGER NOT NULL DEFAULT 0,
  started_at    INTEGER,
  ended_at      INTEGER,
  end_note      TEXT,
  notes         TEXT,
  sort_index    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_care_thread_subject ON care_thread (subject_id);

-- A facility, agency, practice, or insurer. Kept separate from the individuals
-- who work there, because the facility's own details outlive any one person in
-- it and because a person moves between organizations.
CREATE TABLE IF NOT EXISTS organization (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  name          TEXT    NOT NULL,
  kind          TEXT,
  phone         TEXT,
  address       TEXT,
  notes         TEXT,
  -- Archived with its chapter, still searchable. Archiving is not deletion:
  -- an archived contact must still resolve inside a two year old call note,
  -- otherwise that note becomes a call with nobody.
  archived_at   INTEGER
);

-- Someone on the care team. Every person's page assembles their whole history:
-- every call and visit involving them, every question waiting for them.
CREATE TABLE IF NOT EXISTS person (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  -- One field, not first and last. A person may have one name, and splitting
  -- it is a Western assumption that breaks in several of the four languages.
  display_name  TEXT    NOT NULL,
  role_label    TEXT,
  role_template_id TEXT,
  organization_id  TEXT REFERENCES organization (id),
  phone         TEXT,
  email         TEXT,
  shift_note    TEXT,
  notes         TEXT,
  archived_at   INTEGER
);

CREATE INDEX IF NOT EXISTS ix_person_subject ON person (subject_id);
CREATE INDEX IF NOT EXISTS ix_person_org     ON person (organization_id);

-- Which chapter a person belonged to, and in what role there. A nurse at the
-- rehab facility is a different row from the same human at the nursing home.
CREATE TABLE IF NOT EXISTS person_chapter (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  person_id     TEXT    NOT NULL REFERENCES person (id),
  chapter_id    TEXT    NOT NULL REFERENCES chapter (id),
  role_label    TEXT
);

CREATE INDEX IF NOT EXISTS ix_person_chapter_person  ON person_chapter (person_id);
CREATE INDEX IF NOT EXISTS ix_person_chapter_chapter ON person_chapter (chapter_id);

-- THE TRAIL. One chronological record of everything that happened. Entries
-- answer "when", and every entry can also carry a chapter and any number of
-- threads, which is what makes every screen in the app a lens on this one
-- table rather than a separate feature.
--
-- Capture forgives, and that is a functional requirement rather than a nicety.
-- Every field here except kind and the bookkeeping columns is nullable. A
-- half remembered note is a valid note. Dates can be rough, which is what
-- occurred_precision carries.
CREATE TABLE IF NOT EXISTS entry (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  kind          TEXT    NOT NULL CHECK (kind IN (
                  'call', 'visit', 'incident', 'measurement',
                  'question', 'document', 'note', 'transfer', 'milestone'
                )),

  -- When it happened, as distinct from when it was written down. Null means
  -- the person genuinely did not know, which is a real answer and is rendered
  -- as one rather than being guessed at.
  occurred_at   INTEGER,
  -- How precisely occurred_at is meant. 'exact' down to the minute, 'day',
  -- 'week' for "sometime this week", 'month', and 'unknown'. The trail renders
  -- an imprecise date as imprecise rather than pretending to a precision the
  -- person did not have.
  occurred_precision TEXT NOT NULL DEFAULT 'day' CHECK (occurred_precision IN (
                  'exact', 'day', 'week', 'month', 'unknown'
                )),

  title         TEXT,
  body          TEXT,

  chapter_id    TEXT REFERENCES chapter (id),
  incident_id   TEXT REFERENCES incident (id),

  -- Set when the person could not say where this belonged. It sits in the
  -- Unfiled tray until they confirm a home with one tap. The app suggests by
  -- plain word matching and never files anything on its own.
  is_unfiled    INTEGER NOT NULL DEFAULT 0 CHECK (is_unfiled IN (0, 1)),
  suggested_home TEXT,

  pinned_at     INTEGER
);

CREATE INDEX IF NOT EXISTS ix_entry_trail    ON entry (subject_id, occurred_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_entry_kind     ON entry (subject_id, kind, occurred_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_entry_chapter  ON entry (chapter_id, occurred_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_entry_incident ON entry (incident_id, occurred_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_entry_unfiled  ON entry (subject_id) WHERE is_unfiled = 1 AND deleted_at IS NULL;

-- Which threads an entry belongs to. An entry can carry several: a call about
-- a wound during a physical therapy week belongs to both.
CREATE TABLE IF NOT EXISTS entry_thread (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  entry_id      TEXT    NOT NULL REFERENCES entry (id),
  thread_id     TEXT    NOT NULL REFERENCES care_thread (id)
);

CREATE INDEX IF NOT EXISTS ix_entry_thread_entry  ON entry_thread (entry_id);
CREATE INDEX IF NOT EXISTS ix_entry_thread_thread ON entry_thread (thread_id);

-- Who was involved. This is what lets a person's page assemble every call and
-- visit involving them, and it is why archiving a contact must never remove
-- these rows.
CREATE TABLE IF NOT EXISTS entry_person (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  entry_id      TEXT    NOT NULL REFERENCES entry (id),
  person_id     TEXT    NOT NULL REFERENCES person (id),
  -- 'spoke_with', 'left_message_for', 'present', 'mentioned'.
  involvement   TEXT
);

CREATE INDEX IF NOT EXISTS ix_entry_person_entry  ON entry_person (entry_id);
CREATE INDEX IF NOT EXISTS ix_entry_person_person ON entry_person (person_id);

-- The extra fields a call carries. Split out rather than widening entry,
-- because most entries are not calls and a wide sparse table indexes badly at
-- five year scale.
CREATE TABLE IF NOT EXISTS call_detail (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  entry_id      TEXT    NOT NULL REFERENCES entry (id),
  direction     TEXT CHECK (direction IN ('outgoing', 'incoming')),
  number_called TEXT,
  reached       INTEGER CHECK (reached IN (0, 1)),
  duration_minutes INTEGER,
  outcome       TEXT
);

CREATE INDEX IF NOT EXISTS ix_call_detail_entry ON call_detail (entry_id);

CREATE TABLE IF NOT EXISTS visit_detail (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  entry_id      TEXT    NOT NULL REFERENCES entry (id),
  arrived_at    INTEGER,
  left_at       INTEGER,
  location_note TEXT
);

CREATE INDEX IF NOT EXISTS ix_visit_detail_entry ON visit_detail (entry_id);

-- An incident is a thread from first report to resolution. Each call and
-- escalation along the way is an entry pointing here through entry.incident_id,
-- which is what makes the incident readable start to finish and exportable on
-- its own.
--
-- There is deliberately no severity field and no priority field. The app does
-- not rank what happened to someone.
CREATE TABLE IF NOT EXISTS incident (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  title         TEXT    NOT NULL,
  description   TEXT,
  chapter_id    TEXT REFERENCES chapter (id),
  reported_at   INTEGER,
  resolved_at   INTEGER,
  resolution_note TEXT,
  -- Recorded so "incidents over time" can count, and only count. What the
  -- count means is stated as the person's to judge, every time it is shown.
  shift_note    TEXT
);

CREATE INDEX IF NOT EXISTS ix_incident_subject ON incident (subject_id, reported_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_incident_open    ON incident (subject_id) WHERE resolved_at IS NULL AND deleted_at IS NULL;

-- A medication. This is a record, not a tracker. The app does not remind, does
-- not alert, and does not track doses taken, and the medications screen says
-- that plainly on the screen rather than burying it in settings.
CREATE TABLE IF NOT EXISTS medication (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  name          TEXT    NOT NULL,
  -- What the person was told, in the words they were told it in. The app never
  -- parses a dose into a number and a unit, because misparsing a dose is worse
  -- than not parsing one.
  dose_text     TEXT,
  purpose_text  TEXT,
  prescriber_person_id TEXT REFERENCES person (id),
  started_at    INTEGER,
  stopped_at    INTEGER,
  stop_reason   TEXT,
  notes         TEXT,
  -- Shown on the emergency card, which is designed to be handed to a paramedic.
  on_emergency_card INTEGER NOT NULL DEFAULT 0 CHECK (on_emergency_card IN (0, 1))
);

CREATE INDEX IF NOT EXISTS ix_medication_subject ON medication (subject_id) WHERE deleted_at IS NULL;

-- A change to a medication: started, stopped, dose changed, held. This is what
-- the journey view across chapters is built from, and what puts a start marker
-- on a chart.
CREATE TABLE IF NOT EXISTS medication_event (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  medication_id TEXT    NOT NULL REFERENCES medication (id),
  entry_id      TEXT REFERENCES entry (id),
  chapter_id    TEXT REFERENCES chapter (id),
  kind          TEXT    NOT NULL CHECK (kind IN (
                  'started', 'stopped', 'dose_changed', 'held', 'resumed', 'noted'
                )),
  occurred_at   INTEGER,
  dose_text     TEXT,
  note          TEXT
);

CREATE INDEX IF NOT EXISTS ix_medication_event_med ON medication_event (medication_id, occurred_at);

-- A concern flag stays attached to a medication forever. It is not a warning
-- and it is not a judgment: it is the person recording that something was
-- raised, so that it is still findable two years later.
CREATE TABLE IF NOT EXISTS medication_flag (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  medication_id TEXT    NOT NULL REFERENCES medication (id),
  raised_at     INTEGER,
  raised_with_person_id TEXT REFERENCES person (id),
  note          TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_medication_flag_med ON medication_flag (medication_id);

CREATE TABLE IF NOT EXISTS appointment (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  title         TEXT    NOT NULL,
  person_id     TEXT REFERENCES person (id),
  organization_id TEXT REFERENCES organization (id),
  chapter_id    TEXT REFERENCES chapter (id),
  scheduled_at  INTEGER,
  location_note TEXT,
  notes         TEXT,
  attended_at   INTEGER,
  outcome_note  TEXT
);

CREATE INDEX IF NOT EXISTS ix_appointment_upcoming ON appointment (subject_id, scheduled_at) WHERE deleted_at IS NULL;

-- Ask next time. The question inbox, which surfaces on the right appointment's
-- prep sheet.
CREATE TABLE IF NOT EXISTS question (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  text          TEXT    NOT NULL,
  -- Who it is for. A question waiting for the wound nurse should not appear on
  -- the prep sheet for a billing meeting.
  person_id     TEXT REFERENCES person (id),
  role_label    TEXT,
  -- What prompted it, so the question keeps its context months later.
  entry_id      TEXT REFERENCES entry (id),
  medication_id TEXT REFERENCES medication (id),
  asked_at      INTEGER,
  asked_at_appointment_id TEXT REFERENCES appointment (id),
  answer_text   TEXT
);

CREATE INDEX IF NOT EXISTS ix_question_open ON question (subject_id) WHERE asked_at IS NULL AND deleted_at IS NULL;

-- A measure the person chose to track, created from a progress preset or from
-- scratch. The preset's advice_risk is a build instruction rather than a label:
-- for anything marked high the interface must not show normal ranges, must not
-- color code values, must not sort or highlight by value, and must word every
-- field so it records what a clinician said.
CREATE TABLE IF NOT EXISTS measure (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  name          TEXT    NOT NULL,
  preset_id     TEXT,
  unit          TEXT,
  style         TEXT    NOT NULL DEFAULT 'continuous' CHECK (style IN (
                  'continuous', 'milestone_heavy', 'event_log',
                  'observational', 'photo_log', 'categorical'
                )),
  gap_tolerance TEXT    NOT NULL DEFAULT 'moderate',
  -- Carried from the preset so the rendering layer can enforce the content
  -- rules without looking the preset up again.
  advice_risk   TEXT    NOT NULL DEFAULT 'low' CHECK (advice_risk IN ('low', 'medium', 'high')),
  show_medication_markers INTEGER NOT NULL DEFAULT 0 CHECK (show_medication_markers IN (0, 1)),
  notes         TEXT,
  sort_index    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_measure_subject ON measure (subject_id) WHERE deleted_at IS NULL;

-- One data point. There is deliberately no column for whether a value is
-- normal, high, low, in range, or concerning, and none may ever be added. The
-- app records what was measured and who said it. It does not judge it.
--
-- A gap between measurements is a gap. It is never interpolated across, never
-- annotated as a lapse, and never implied to be a failure.
CREATE TABLE IF NOT EXISTS measurement (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  measure_id    TEXT    NOT NULL REFERENCES measure (id),
  entry_id      TEXT REFERENCES entry (id),
  chapter_id    TEXT REFERENCES chapter (id),
  occurred_at   INTEGER,
  occurred_precision TEXT NOT NULL DEFAULT 'day' CHECK (occurred_precision IN (
                  'exact', 'day', 'week', 'month', 'unknown'
                )),
  -- Numeric where there is a number, text where the person was given words.
  -- Both may be present: a wound stage is words a clinician said, and the
  -- field label says so.
  value_number  REAL,
  value_text    TEXT,
  unit          TEXT,
  -- Who provided this, since a value the family measured and a value a
  -- clinician stated are different things and the record must not blur them.
  reported_by_person_id TEXT REFERENCES person (id),
  source        TEXT CHECK (source IN ('family', 'clinician', 'device', 'unknown')),
  note          TEXT
);

CREATE INDEX IF NOT EXISTS ix_measurement_series ON measurement (measure_id, occurred_at) WHERE deleted_at IS NULL;

-- A dated event on the person's path, shown on the milestone arc as a
-- continuous trail rather than as a filter over entries.
CREATE TABLE IF NOT EXISTS milestone (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  label         TEXT    NOT NULL,
  occurred_at   INTEGER,
  chapter_id    TEXT REFERENCES chapter (id),
  measure_id    TEXT REFERENCES measure (id),
  note          TEXT
);

CREATE INDEX IF NOT EXISTS ix_milestone_subject ON milestone (subject_id, occurred_at);

CREATE TABLE IF NOT EXISTS document (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  title         TEXT    NOT NULL,
  category      TEXT,
  chapter_id    TEXT REFERENCES chapter (id),
  entry_id      TEXT REFERENCES entry (id),
  project_id    TEXT REFERENCES project (id),
  received_at   INTEGER,
  -- Where the paper original physically is. This is one of the most useful
  -- fields in the app and it exists because the digital copy is rarely the one
  -- a clerk will accept.
  original_location TEXT,
  notes         TEXT
);

CREATE INDEX IF NOT EXISTS ix_document_subject ON document (subject_id) WHERE deleted_at IS NULL;

-- Content addressed. The file on disk is named by the hash of its bytes, so
-- identical bytes are the same file, an attachment can never conflict, and a
-- corrupt transfer is detectable by rehashing.
--
-- Per attachment cap: 25 MB, with the limit stated before the person meets it
-- rather than after. Total attachment size is warned about at 4 GB and never
-- blocked, because it is their data and their phone. See DECISIONS.md.
CREATE TABLE IF NOT EXISTS attachment (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  -- Lowercase hex sha256 of the file's bytes. Also the filename on disk.
  sha256        TEXT    NOT NULL,
  original_filename TEXT,
  mime_type     TEXT,
  byte_size     INTEGER NOT NULL,
  -- What it is attached to. Exactly one of these is set.
  document_id   TEXT REFERENCES document (id),
  entry_id      TEXT REFERENCES entry (id),
  bill_id       TEXT REFERENCES bill (id),
  project_id    TEXT REFERENCES project (id),
  measurement_id TEXT REFERENCES measurement (id),
  caption       TEXT
);

CREATE INDEX IF NOT EXISTS ix_attachment_sha      ON attachment (sha256);
CREATE INDEX IF NOT EXISTS ix_attachment_document ON attachment (document_id);
CREATE INDEX IF NOT EXISTS ix_attachment_entry    ON attachment (entry_id);

CREATE TABLE IF NOT EXISTS bill (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  description   TEXT    NOT NULL,
  organization_id TEXT REFERENCES organization (id),
  chapter_id    TEXT REFERENCES chapter (id),
  -- Stored in minor units as an integer. Floating point money is a defect
  -- waiting for a rounding boundary, and this record may be read out in a
  -- dispute.
  amount_minor  INTEGER,
  currency      TEXT NOT NULL DEFAULT 'USD',
  received_at   INTEGER,
  due_at        INTEGER,
  state         TEXT NOT NULL DEFAULT 'needs_attention' CHECK (state IN (
                  'needs_attention', 'disputed', 'waiting_on_insurance', 'paid', 'closed'
                )),
  state_note    TEXT,
  paid_at       INTEGER,
  notes         TEXT
);

CREATE INDEX IF NOT EXISTS ix_bill_subject ON bill (subject_id, received_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_bill_state   ON bill (subject_id, state) WHERE deleted_at IS NULL;

-- A running cost sheet for any long expense, not only a facility.
CREATE TABLE IF NOT EXISTS cost_sheet (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  name          TEXT    NOT NULL,
  chapter_id    TEXT REFERENCES chapter (id),
  currency      TEXT NOT NULL DEFAULT 'USD',
  notes         TEXT
);

CREATE TABLE IF NOT EXISTS cost_entry (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  cost_sheet_id TEXT    NOT NULL REFERENCES cost_sheet (id),
  description   TEXT,
  amount_minor  INTEGER NOT NULL,
  occurred_at   INTEGER,
  bill_id       TEXT REFERENCES bill (id)
);

CREATE INDEX IF NOT EXISTS ix_cost_entry_sheet ON cost_entry (cost_sheet_id, occurred_at);

-- What was asked, of whom, when, and how it was acknowledged.
--
-- The tag is load bearing rather than decorative. 'federal' refers specifically
-- to federal rules for nursing homes participating in Medicare or Medicaid. It
-- does not apply to assisted living, home care agencies, or hospitals, and the
-- interface must not imply the backing carries over. Overstating this would
-- send a family into a meeting with a claim that does not hold.
CREATE TABLE IF NOT EXISTS standing_instruction (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  template_id   TEXT,
  name          TEXT    NOT NULL,
  wording       TEXT    NOT NULL,
  tag           TEXT    NOT NULL CHECK (tag IN ('federal', 'request')),
  chapter_id    TEXT REFERENCES chapter (id),
  given_to_person_id TEXT REFERENCES person (id),
  given_at      INTEGER,
  acknowledged_at INTEGER,
  acknowledged_how TEXT,
  ended_at      INTEGER,
  notes         TEXT
);

CREATE INDEX IF NOT EXISTS ix_standing_instruction_subject ON standing_instruction (subject_id) WHERE deleted_at IS NULL;

-- A documented violation of an instruction, linked to whatever evidences it.
-- The app counts violations. It does not conclude anything from the count.
CREATE TABLE IF NOT EXISTS instruction_violation (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  instruction_id TEXT   NOT NULL REFERENCES standing_instruction (id),
  occurred_at   INTEGER,
  entry_id      TEXT REFERENCES entry (id),
  incident_id   TEXT REFERENCES incident (id),
  bill_id       TEXT REFERENCES bill (id),
  note          TEXT
);

CREATE INDEX IF NOT EXISTS ix_instruction_violation ON instruction_violation (instruction_id, occurred_at);

-- A long bureaucratic process, kept separate from the notebook because it has
-- its own contacts, its own timeline, and its own end.
CREATE TABLE IF NOT EXISTS project (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  name          TEXT    NOT NULL,
  template_id   TEXT,
  chapter_id    TEXT REFERENCES chapter (id),
  status        TEXT NOT NULL DEFAULT 'active' CHECK (status IN (
                  'active', 'waiting', 'stalled', 'done', 'abandoned'
                )),
  -- These processes stall on other people constantly, so what is being waited
  -- on is a first class field rather than a note.
  waiting_on    TEXT,
  waiting_since INTEGER,
  started_at    INTEGER,
  finished_at   INTEGER,
  notes         TEXT
);

CREATE INDEX IF NOT EXISTS ix_project_subject ON project (subject_id, status) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS project_step (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  project_id    TEXT    NOT NULL REFERENCES project (id),
  text          TEXT    NOT NULL,
  sort_index    INTEGER NOT NULL DEFAULT 0,
  completed_at  INTEGER,
  note          TEXT
);

CREATE INDEX IF NOT EXISTS ix_project_step_project ON project_step (project_id, sort_index);

CREATE TABLE IF NOT EXISTS project_person (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  project_id    TEXT    NOT NULL REFERENCES project (id),
  person_id     TEXT    NOT NULL REFERENCES person (id),
  role_label    TEXT
);

CREATE INDEX IF NOT EXISTS ix_project_person_project ON project_person (project_id);

-- Designed to be handed to a paramedic. One row per subject.
CREATE TABLE IF NOT EXISTS emergency_card (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  subject_id    TEXT    NOT NULL REFERENCES subject (id),
  allergies     TEXT,
  blood_type    TEXT,
  conditions    TEXT,
  -- Recorded as what the signed paperwork says, with where the original is
  -- kept, because the card is useless if the paper cannot be produced.
  resuscitation_status TEXT,
  resuscitation_document_location TEXT,
  decision_maker_person_id TEXT REFERENCES person (id),
  decision_maker_document_location TEXT,
  insurance_note TEXT,
  other_notes   TEXT
);

CREATE INDEX IF NOT EXISTS ix_emergency_card_subject ON emergency_card (subject_id);

CREATE TABLE IF NOT EXISTS emergency_contact (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  emergency_card_id TEXT NOT NULL REFERENCES emergency_card (id),
  person_id     TEXT REFERENCES person (id),
  display_name  TEXT    NOT NULL,
  phone         TEXT,
  relationship  TEXT,
  sort_index    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_emergency_contact_card ON emergency_contact (emergency_card_id, sort_index);

-- A template the person edited, duplicated, or built from scratch. Custom
-- templates save alongside the built-in ones and are exported with everything
-- else, because a template someone wrote is their work.
CREATE TABLE IF NOT EXISTS custom_template (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  kind          TEXT    NOT NULL CHECK (kind IN (
                  'situation', 'project', 'progress', 'standing_instruction'
                )),
  -- The built-in this was derived from, where it was derived from one. Null
  -- means built from scratch.
  derived_from_id TEXT,
  name          TEXT    NOT NULL,
  -- The template body, in the same shape as the bundled JSON, so one loader
  -- reads both.
  body_json     TEXT    NOT NULL
);

-- The generic connection table. Strong relationships above are foreign keys.
-- This carries the rest, which is what makes "one tap assembles everything
-- connected to this" a single query rather than a union of thirty.
--
-- No dead ends is the rule the app is built on: the person must never have to
-- remember where something was filed, because every path leads to it.
CREATE TABLE IF NOT EXISTS link (
  id            TEXT    NOT NULL PRIMARY KEY,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  deleted_at    INTEGER,
  origin_device TEXT    NOT NULL,
  rev           INTEGER NOT NULL DEFAULT 1,

  source_table  TEXT    NOT NULL,
  source_id     TEXT    NOT NULL,
  target_table  TEXT    NOT NULL,
  target_id     TEXT    NOT NULL,
  relation      TEXT,
  note          TEXT
);

CREATE INDEX IF NOT EXISTS ix_link_source ON link (source_table, source_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_link_target ON link (target_table, target_id) WHERE deleted_at IS NULL;


-- ---------------------------------------------------------------------------
-- LIVE VIEWS
--
-- Every read goes through one of these. Reading tombstones requires naming the
-- base table deliberately, which is the point: it makes the safe path the easy
-- one and the unsafe path visible in review and in the static check.
-- ---------------------------------------------------------------------------

CREATE VIEW IF NOT EXISTS live_subject               AS SELECT * FROM subject               WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_chapter               AS SELECT * FROM chapter               WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_care_thread           AS SELECT * FROM care_thread           WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_organization          AS SELECT * FROM organization          WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_person                AS SELECT * FROM person                WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_person_chapter        AS SELECT * FROM person_chapter        WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_entry                 AS SELECT * FROM entry                 WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_entry_thread          AS SELECT * FROM entry_thread          WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_entry_person          AS SELECT * FROM entry_person          WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_call_detail           AS SELECT * FROM call_detail           WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_visit_detail          AS SELECT * FROM visit_detail          WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_incident              AS SELECT * FROM incident              WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_medication            AS SELECT * FROM medication            WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_medication_event      AS SELECT * FROM medication_event      WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_medication_flag       AS SELECT * FROM medication_flag       WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_appointment           AS SELECT * FROM appointment           WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_question              AS SELECT * FROM question              WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_measure               AS SELECT * FROM measure               WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_measurement           AS SELECT * FROM measurement           WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_milestone             AS SELECT * FROM milestone             WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_document              AS SELECT * FROM document              WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_attachment            AS SELECT * FROM attachment            WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_bill                  AS SELECT * FROM bill                  WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_cost_sheet            AS SELECT * FROM cost_sheet            WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_cost_entry            AS SELECT * FROM cost_entry            WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_standing_instruction  AS SELECT * FROM standing_instruction  WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_instruction_violation AS SELECT * FROM instruction_violation WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_project               AS SELECT * FROM project               WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_project_step          AS SELECT * FROM project_step          WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_project_person        AS SELECT * FROM project_person        WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_emergency_card        AS SELECT * FROM emergency_card        WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_emergency_contact     AS SELECT * FROM emergency_contact     WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_custom_template       AS SELECT * FROM custom_template       WHERE deleted_at IS NULL;
CREATE VIEW IF NOT EXISTS live_link                  AS SELECT * FROM link                  WHERE deleted_at IS NULL;


-- ---------------------------------------------------------------------------
-- CHANGE LOG TRIGGERS
--
-- Two per user data table. They are what make "every write appends to the
-- change log in the same transaction" a property of the database rather than a
-- promise the application layer keeps.
--
-- The insert trigger records 'insert'. The update trigger records 'delete'
-- when deleted_at goes from null to set, since deletion here is an update, and
-- 'update' otherwise. A row that is undeleted records an 'update', which is
-- correct: the peer needs to know the tombstone was lifted.
--
-- device_id comes from app_meta, which the application writes once at first
-- launch. If it is missing the trigger writes 'unknown-device' rather than
-- failing the write, because losing the person's entry is worse than losing
-- the provenance of it.
-- ---------------------------------------------------------------------------

CREATE TRIGGER IF NOT EXISTS trg_subject_insert AFTER INSERT ON subject
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('subject', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_subject_update AFTER UPDATE ON subject
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('subject', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_chapter_insert AFTER INSERT ON chapter
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('chapter', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_chapter_update AFTER UPDATE ON chapter
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('chapter', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_care_thread_insert AFTER INSERT ON care_thread
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('care_thread', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_care_thread_update AFTER UPDATE ON care_thread
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('care_thread', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_organization_insert AFTER INSERT ON organization
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('organization', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_organization_update AFTER UPDATE ON organization
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('organization', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_person_insert AFTER INSERT ON person
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('person', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_person_update AFTER UPDATE ON person
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('person', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_person_chapter_insert AFTER INSERT ON person_chapter
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('person_chapter', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_person_chapter_update AFTER UPDATE ON person_chapter
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('person_chapter', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_entry_insert AFTER INSERT ON entry
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('entry', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_entry_update AFTER UPDATE ON entry
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('entry', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_entry_thread_insert AFTER INSERT ON entry_thread
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('entry_thread', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_entry_thread_update AFTER UPDATE ON entry_thread
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('entry_thread', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_entry_person_insert AFTER INSERT ON entry_person
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('entry_person', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_entry_person_update AFTER UPDATE ON entry_person
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('entry_person', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_call_detail_insert AFTER INSERT ON call_detail
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('call_detail', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_call_detail_update AFTER UPDATE ON call_detail
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('call_detail', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_visit_detail_insert AFTER INSERT ON visit_detail
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('visit_detail', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_visit_detail_update AFTER UPDATE ON visit_detail
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('visit_detail', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_incident_insert AFTER INSERT ON incident
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('incident', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_incident_update AFTER UPDATE ON incident
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('incident', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_medication_insert AFTER INSERT ON medication
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('medication', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_medication_update AFTER UPDATE ON medication
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('medication', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_medication_event_insert AFTER INSERT ON medication_event
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('medication_event', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_medication_event_update AFTER UPDATE ON medication_event
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('medication_event', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_medication_flag_insert AFTER INSERT ON medication_flag
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('medication_flag', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_medication_flag_update AFTER UPDATE ON medication_flag
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('medication_flag', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_appointment_insert AFTER INSERT ON appointment
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('appointment', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_appointment_update AFTER UPDATE ON appointment
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('appointment', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_question_insert AFTER INSERT ON question
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('question', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_question_update AFTER UPDATE ON question
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('question', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_measure_insert AFTER INSERT ON measure
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('measure', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_measure_update AFTER UPDATE ON measure
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('measure', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_measurement_insert AFTER INSERT ON measurement
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('measurement', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_measurement_update AFTER UPDATE ON measurement
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('measurement', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_milestone_insert AFTER INSERT ON milestone
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('milestone', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_milestone_update AFTER UPDATE ON milestone
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('milestone', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_document_insert AFTER INSERT ON document
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('document', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_document_update AFTER UPDATE ON document
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('document', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_attachment_insert AFTER INSERT ON attachment
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('attachment', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_attachment_update AFTER UPDATE ON attachment
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('attachment', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_bill_insert AFTER INSERT ON bill
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('bill', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_bill_update AFTER UPDATE ON bill
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('bill', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_cost_sheet_insert AFTER INSERT ON cost_sheet
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('cost_sheet', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_cost_sheet_update AFTER UPDATE ON cost_sheet
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('cost_sheet', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_cost_entry_insert AFTER INSERT ON cost_entry
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('cost_entry', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_cost_entry_update AFTER UPDATE ON cost_entry
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('cost_entry', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_standing_instruction_insert AFTER INSERT ON standing_instruction
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('standing_instruction', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_standing_instruction_update AFTER UPDATE ON standing_instruction
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('standing_instruction', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_instruction_violation_insert AFTER INSERT ON instruction_violation
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('instruction_violation', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_instruction_violation_update AFTER UPDATE ON instruction_violation
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('instruction_violation', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_project_insert AFTER INSERT ON project
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('project', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_project_update AFTER UPDATE ON project
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('project', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_project_step_insert AFTER INSERT ON project_step
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('project_step', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_project_step_update AFTER UPDATE ON project_step
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('project_step', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_project_person_insert AFTER INSERT ON project_person
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('project_person', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_project_person_update AFTER UPDATE ON project_person
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('project_person', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_emergency_card_insert AFTER INSERT ON emergency_card
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('emergency_card', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_emergency_card_update AFTER UPDATE ON emergency_card
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('emergency_card', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_emergency_contact_insert AFTER INSERT ON emergency_contact
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('emergency_contact', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_emergency_contact_update AFTER UPDATE ON emergency_contact
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('emergency_contact', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_custom_template_insert AFTER INSERT ON custom_template
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('custom_template', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_custom_template_update AFTER UPDATE ON custom_template
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('custom_template', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_link_insert AFTER INSERT ON link
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('link', NEW.id, 'insert', NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;

CREATE TRIGGER IF NOT EXISTS trg_link_update AFTER UPDATE ON link
BEGIN
  INSERT INTO change_log (table_name, row_id, op, rev, changed_at, device_id)
  VALUES ('link', NEW.id,
          CASE WHEN OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL
               THEN 'delete' ELSE 'update' END,
          NEW.rev, NEW.updated_at,
          COALESCE((SELECT value FROM app_meta WHERE key = 'device_id'), 'unknown-device'));
END;
