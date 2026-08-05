# Schema

What every field means, for anyone embedding this data in software.

`schema_version` is `1` in all three files. Bump it on any breaking change and note the change here.

## Shared

Every file has:

| Field | Meaning |
|---|---|
| `schema_version` | Integer. Breaking changes only. |
| `type` | Which of the four template kinds this file holds. |
| `content_license` | `CC BY-SA 4.0` for all template content. |
| `posture` | Short strings the UI should display verbatim: the general guide line, the record only line, and the state variance line. Do not paraphrase these in the interface. |

Every template has a stable `id`. **Never change an id after release.** Ids are the join key for translations and for anything a user has already created from a template.

## situations.json

One entry per care setting.

| Field | Type | Meaning |
|---|---|---|
| `id` | string | Stable key. |
| `name` | string | Shown in the setup picker. |
| `subtitle` | string | One line under the name, so the person can tell two similar settings apart. |
| `phase` | 1, 2, or 3 | Build priority. Phase 1 covers the largest number of caregivers and ships first. **Also the ordering in the setup picker**, so the most common settings are the first ones a person reads. |
| `group` | `facility`, `home`, `treatment`, `comfort` | Which heading this sits under in the setup picker. Fourteen options presented flat is a wall to someone standing in a hallway, so they are grouped by where the care is happening, which is the thing the person already knows. Lives in the data rather than in one platform's code, so both group identically. |
| `forward` | array of section ids | Notebook sections shown expanded for this setting. |
| `folded` | array of section ids | Sections collapsed until the person opens them. Not hidden. |
| `roles` | array of `{id, label}` | Contact role slots to offer when adding a person. Suggestions, not a fixed list. |
| `threads` | array of `{id, label}` | Care threads offered as toggles at setup. The person picks which are actually running. |
| `checklist` | array of strings | First days checklist. Administrative actions only. |
| `documents` | array of strings | Document slots to create, each expecting a photo and a note about where the original lives. |
| `burden` | string | One sentence naming what is hard about this setting. Use it as supporting text at setup, so the person feels understood rather than processed. |
| `state_variance` | bool, optional | When true, show the state variance line from `posture`. |
| `sensitive` | bool, optional | Handle copy with extra care. Currently only hospice. Avoid cheerful phrasing and avoid euphemism. |
| `localization_note` | string, optional | Instruction for translators. Not user facing. |

Valid section ids: `today`, `care_team`, `medications`, `appointments`, `chapters`, `threads`, `trail`, `progress`, `documents`, `money`, `standing_instructions`, `ask_next_time`, `emergency_card`, `projects`.

## projects.json

One entry per long bureaucratic process.

| Field | Type | Meaning |
|---|---|---|
| `id`, `name`, `subtitle`, `phase` | | As above. |
| `category` | one of `paying`, `challenge`, `moving`, `papers` | **What the person is trying to do**, which is how the picker groups them. Not what kind of office it involves and not which law it sits under: somebody looking for a process is thinking "they cut her off and I want to appeal it", not "this is a Medicare matter". The four are named in the locale catalogs under `projects.category.*` and the picker keeps them in the order listed here. **Not the same as `phase`**, which is build order and never reaches a screen. |
| `roles` | array of `{id, label}` | Contacts specific to this project, kept separate from the medical care team. |
| `steps` | array of strings | **The starting steps**, the third of the five defaults. A short editable list of what people in this situation usually gather or arrange. **Suggestions of structure, not instructions to act**: every one can be deleted, and the app never says do this now. Order is a suggestion. |
| `lead` | one of `standing`, `date`, `steps` | **Which of the three answers the project opens with**, `DESIGN.md` 20.1 and 20.3, and therefore which of the three shapes it takes: the long road, the closing window, or the busy stretch. **A default and never a cage**: it is one control on the project's setup screen and changes with no penalty. Closed, because the schema's own CHECK refuses anything else. |
| `stages` | array of strings | **The named stretches of the road**, drawn as the road strip. At least two, because a road with one waypoint says nothing. Copied onto the project at setup, after which they can be renamed, added to, or removed and the road redraws. |
| `date_kinds` | array of strings | **The kinds of date this situation tends to have**, offered as chips when a date is recorded. Never a closed set: recording a date of a kind not listed here is allowed. |
| `documents` | array of strings | **The usual papers**, the fifth default. Named placeholders, empty until filled. **An empty placeholder reads "not yet", never as an error.** |
| `waiting_on_prompts` | array of strings | Offered as choices for the project's "waiting on" field, since these processes stall on other people constantly. |
| `failure_points` | array of strings | Where this usually goes wrong. Display as context, never as a warning banner. |
| `timeline_shape` | string | Honest expectation setting in plain words. Deliberately vague: no fixed day counts, since they vary and go stale. |
| `state_variance` | bool, optional | When true, show the state variance line. |
| `rights_note` | string, optional | A factual statement that a federal rule exists. Facts only. |
| `advice_note` | string, optional | Explicit reminder that this is not legal or financial advice. Show it prominently. |
| `localization_note` | string, optional | For translators. |

## progress-and-instructions.json

### `progress_presets`

| Field | Type | Meaning |
|---|---|---|
| `id`, `name` | | Stable key and display name. |
| `unit_options` | array | Units to offer. Empty array means no units, notes only. |
| `cadence` | string | Suggested frequency, in words. Never enforced, never a reminder. |
| `medication_markers` | bool | Whether to offer marking medication start dates on this chart. |
| `style` | string | `continuous`, `milestone_heavy`, `event_log`, `observational`, `photo_log`, or `categorical`. Drives which chart or list view is used. |
| `gap_tolerance` | string | `high`, `moderate`, `low`, `cycle_based`, `not_applicable`. Charts must render gaps as gaps. Never interpolate across a gap and never imply a missed entry is a failure. |
| `fields` | array | The fields to capture. All optional at entry time. |
| `*_options` | array, optional | Choice lists for categorical fields. |
| `notes` | string, optional | Guidance for whoever builds the screen. |
| `advice_risk` | `low`, `medium`, `high` | How easily this measurement could drift into looking like medical guidance. |

**`advice_risk` is a build instruction, not a label.** For anything marked `high`, the implementation must not show normal ranges, must not color code values, must not sort or highlight by value, and must word every field so it records what a clinician said rather than what the family concluded. Wound staging and growth measurements are the clearest cases: the family records the number or the words they were given, and the field label must say so.

### `standing_instruction_tags`

Two tags, `federal` and `request`, each with a short `label` for the badge and a longer `explainer`. Show the explainer where the person can reach it easily, because the difference between the two is the entire point.

### `standing_instructions`

| Field | Type | Meaning |
|---|---|---|
| `id`, `name` | | Stable key and short display name. |
| `wording` | string | The editable starter text. Square brackets mark blanks the person fills in. |
| `tag` | `federal` or `request` | Which badge to show. |
| `basis` | string | Plain explanation of what backs it up, including where the backing stops. Never expand this into a legal claim. |
| `ask_for` | string | What acknowledgment to request, which is the part that makes the instruction stick. |

The `federal` tag refers specifically to federal rules for nursing homes that participate in Medicare or Medicaid. It does **not** apply to assisted living, home care agencies, or hospitals. If the person's current setting is not a nursing home, the interface must not imply the federal backing carries over.

## Adding a template

1. Add the entry to the right JSON file with a new stable `id`.
2. Keep every string short and plain enough to translate.
3. Check it against the six rules in the README, especially: structure not advice, original wording, no volatile facts.
4. Run `python3 build-catalog.py`.
5. Confirm the catalog output reads correctly and the count in the build output went up.
