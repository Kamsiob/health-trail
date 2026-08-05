#!/usr/bin/env python3
"""Validate the 57 bundled templates against templates/SCHEMA.md.

Two kinds of check, and the second matters more than the first.

Structural: every template has the fields its kind requires, ids are unique and
present, enumerated fields hold a documented value, and the counts match what
the catalog claims.

Content safety: the rules in templates/README.md and PROJECT-DELTAS.md section 6
that make this content safe to hand to a stressed family. Structure never
advice. No volatile facts that go stale and quietly become wrong. No ranges,
thresholds, or judgments on any measurement, anywhere, ever.

Exit 0 when clean, 1 with a list of failures otherwise.

Kamsiob, AGPL-3.0.
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "templates" / "data"

EXPECTED_COUNTS = {
    "situations": 14,
    "projects": 16,
    "progress_presets": 16,
    "standing_instructions": 11,
}

VALID_SECTIONS = {
    "today", "care_team", "medications", "appointments", "chapters", "threads",
    "trail", "progress", "documents", "money", "standing_instructions",
    "ask_next_time", "emergency_card", "projects",
}

VALID_STYLES = {
    "continuous", "milestone_heavy", "event_log", "observational",
    "photo_log", "categorical",
}

VALID_GAP_TOLERANCE = {"high", "moderate", "low", "cycle_based", "not_applicable"}
VALID_ADVICE_RISK = {"low", "medium", "high"}
VALID_TAGS = {"federal", "request"}

SITUATION_REQUIRED = [
    "id", "name", "subtitle", "phase", "forward", "folded", "roles",
    "threads", "checklist", "documents", "burden",
]
PROJECT_REQUIRED = [
    "id", "name", "subtitle", "category", "phase", "roles", "steps", "documents",
    "waiting_on_prompts", "failure_points", "timeline_shape",
    # The five template defaults, DESIGN.md 20.4. Three of them are these; the
    # other two are `steps`, which is the starting steps, and `documents`, which
    # is the usual papers. **A project template is a bundle of five defaults,
    # nothing more and nothing less**, so a template missing one of them
    # produces a project the grid cannot draw.
    "lead", "stages", "date_kinds",
]

# Which of the three answers a project opens with, DESIGN.md 20.1 and 20.3.
# Closed, for the same reason `category` is: the database CHECK refuses anything
# else, so a typo here is a template that cannot start a project at all.
PROJECT_LEADS = ("standing", "date", "steps")

# What the person is trying to do, which is how the picker groups the sixteen.
# **Closed rather than free text**, because a category the app has no label for
# renders as a raw key on screen, and a seventeenth spelling of an existing one
# splits a group in two without anybody noticing. `templates/SCHEMA.md`.
PROJECT_CATEGORIES = ("paying", "challenge", "moving", "papers")
PRESET_REQUIRED = [
    "id", "name", "unit_options", "cadence", "medication_markers", "style",
    "gap_tolerance", "fields", "advice_risk",
]
INSTRUCTION_REQUIRED = ["id", "name", "wording", "tag", "basis", "ask_for"]

# Volatile facts. Anything here goes stale and quietly becomes wrong, which is
# worse than being absent, because a family acts on it.
VOLATILE = [
    (r"\$\s?\d", "a dollar amount"),
    (r"\b\d{3}[-.\s]\d{3}[-.\s]\d{4}\b", "a phone number"),
    (r"\b1[-.\s]?800[-.\s]", "a toll free number"),
    (r"\bwithin \d+ (?:days|business days|hours|weeks)\b", "a fixed deadline in days"),
    (r"\b(?:19|20)\d{2}\b", "a specific year"),
    (r"\bform\s+[A-Z]{1,4}[-\s]?\d{3,}", "a specific form number"),
    (r"\bwww\.|https?://", "a URL"),
]

# Advice. The templates configure what to write down. They never say what to do
# medically or legally.
#
# "you must" is anchored to the start of a sentence deliberately. As an opening
# frame it is the app telling someone what to do, which is banned. Mid sentence
# it usually describes an obligation that already exists in the world, as in
# "write down what reports you must file afterward", which is structure: it says
# what to record, not what to do.
ADVICE = [
    (r"\byou should\b", "you should"),
    (r"(?:^|(?<=[.;!?] ))you must\b", "you must, as an instruction rather than a description"),
    (r"\bwe recommend\b", "we recommend"),
    (r"\bit is recommended\b", "it is recommended"),
    (r"\bmake sure to\b", "make sure to"),
    (r"\bdo not forget\b", "do not forget"),
    (r"\bbe sure to\b", "be sure to"),
    (r"\bconsider asking\b", "consider asking"),
    (r"\bit is important to\b", "it is important to"),
    (r"\bthe best way to\b", "the best way to"),
    (r"\bwe suggest\b", "we suggest"),
]

# Ranges, thresholds, and judgments on a measurement. Banned everywhere, and
# specifically checked on the progress presets, which is where they would creep
# in first.
JUDGMENT = [
    (r"\bnormal range\b", "normal range"),
    (r"\bnormal (?:value|level|reading)", "a normal value"),
    (r"\btarget (?:range|value|weight|number)", "a target"),
    (r"\bhealthy (?:range|weight|level)", "a healthy range"),
    (r"\bshould be (?:above|below|between|around|at least)", "a threshold"),
    (r"\b(?:too high|too low)\b", "a judgment"),
    (r"\bconcerning (?:level|value|reading)", "a judgment"),
    (r"\bwarning (?:sign|level)", "a warning"),
    (r"\bred flag\b", "a warning"),
    (r"\bdanger(?:ous)?\b", "a warning"),
    (r"\babnormal\b", "a judgment"),
    (r"\bideal(?:ly)?\b", "a judgment"),
]


def walk_strings(value, path="")  :
    """Yield every string in a nested structure with a path to it."""
    if isinstance(value, str):
        yield path, value
    elif isinstance(value, dict):
        for key, sub in value.items():
            yield from walk_strings(sub, f"{path}.{key}" if path else key)
    elif isinstance(value, list):
        for index, sub in enumerate(value):
            yield from walk_strings(sub, f"{path}[{index}]")


def check_patterns(text, patterns, ignore=()):
    for pattern, label in patterns:
        match = re.search(pattern, text, re.IGNORECASE)
        if match and match.group(0).lower() not in ignore:
            return label, match.group(0)
    return None


class Validator:
    def __init__(self):
        self.failures = []
        self.checked = 0

    def fail(self, where, message):
        self.failures.append(f"{where}: {message}")

    def shared_header(self, name, data, expected_type):
        if data.get("schema_version") != 1:
            self.fail(name, f"schema_version is {data.get('schema_version')!r}, expected 1")
        if data.get("type") != expected_type:
            self.fail(name, f"type is {data.get('type')!r}, expected {expected_type!r}")
        if data.get("content_license") != "CC BY-SA 4.0":
            self.fail(name, f"content_license is {data.get('content_license')!r}")
        posture = data.get("posture")
        if not isinstance(posture, dict) or not posture:
            self.fail(name, "posture is missing or empty")

    # Fields where an empty list is a documented, meaningful value rather than an
    # omission. SCHEMA.md: unit_options "Empty array means no units, notes only."
    MAY_BE_EMPTY = {"unit_options", "folded", "forward"}

    def required(self, where, item, fields):
        for field in fields:
            if field not in item:
                self.fail(where, f"missing required field {field!r}")
            elif field in self.MAY_BE_EMPTY:
                continue
            elif item[field] in ("", [], None):
                self.fail(where, f"required field {field!r} is empty")

    def ids_unique(self, name, items):
        seen = {}
        for index, item in enumerate(items):
            identifier = item.get("id")
            if not identifier:
                self.fail(f"{name}[{index}]", "has no id, and ids are the join key for translations")
                continue
            if not re.fullmatch(r"[a-z0-9_]+", identifier):
                self.fail(f"{name}:{identifier}", "id should be lowercase with underscores, so it is stable and safe as a key")
            if identifier in seen:
                self.fail(f"{name}:{identifier}", f"duplicate id, first seen at index {seen[identifier]}")
            seen[identifier] = index

    def content_safety(self, where, item, judgment=False):
        for path, text in walk_strings(item):
            # localization_note is an instruction to a translator, never shown.
            if "localization_note" in path or path.endswith("notes"):
                continue
            self.checked += 1
            found = check_patterns(text, VOLATILE)
            if found:
                label, snippet = found
                self.fail(f"{where}.{path}", f"volatile fact, {label}: {snippet!r}")
            found = check_patterns(text, ADVICE)
            if found:
                label, snippet = found
                self.fail(f"{where}.{path}", f"reads as advice rather than structure: {snippet!r}")
            if judgment:
                found = check_patterns(text, JUDGMENT)
                if found:
                    label, snippet = found
                    self.fail(f"{where}.{path}", f"{label} on a measurement: {snippet!r}")

    def situations(self):
        data = json.loads((DATA / "situations.json").read_text(encoding="utf-8"))
        self.shared_header("situations.json", data, "situation_templates")
        items = data.get("templates", [])
        if len(items) != EXPECTED_COUNTS["situations"]:
            self.fail("situations.json", f"has {len(items)} templates, expected {EXPECTED_COUNTS['situations']}")
        self.ids_unique("situations", items)
        for item in items:
            where = f"situations:{item.get('id', '?')}"
            self.required(where, item, SITUATION_REQUIRED)
            if item.get("phase") not in (1, 2, 3):
                self.fail(where, f"phase is {item.get('phase')!r}, expected 1, 2, or 3")
            for key in ("forward", "folded"):
                for section in item.get(key, []):
                    if section not in VALID_SECTIONS:
                        self.fail(where, f"{key} names unknown section {section!r}")
            overlap = set(item.get("forward", [])) & set(item.get("folded", []))
            if overlap:
                self.fail(where, f"section in both forward and folded: {sorted(overlap)}")
            for key in ("roles", "threads"):
                for entry in item.get(key, []):
                    if not isinstance(entry, dict) or "id" not in entry or "label" not in entry:
                        self.fail(where, f"{key} entry is not an object with id and label: {entry!r}")
            self.content_safety(where, item)

    def projects(self):
        data = json.loads((DATA / "projects.json").read_text(encoding="utf-8"))
        self.shared_header("projects.json", data, "project_templates")
        items = data.get("templates", [])
        if len(items) != EXPECTED_COUNTS["projects"]:
            self.fail("projects.json", f"has {len(items)} templates, expected {EXPECTED_COUNTS['projects']}")
        self.ids_unique("projects", items)
        for item in items:
            where = f"projects:{item.get('id', '?')}"
            self.required(where, item, PROJECT_REQUIRED)
            if item.get("phase") not in (1, 2, 3):
                self.fail(where, f"phase is {item.get('phase')!r}, expected 1, 2, or 3")
            if item.get("category") not in PROJECT_CATEGORIES:
                self.fail(
                    where,
                    f"category is {item.get('category')!r}, expected one of "
                    f"{', '.join(PROJECT_CATEGORIES)}",
                )
            if item.get("lead") not in PROJECT_LEADS:
                self.fail(
                    where,
                    f"lead is {item.get('lead')!r}, expected one of "
                    f"{', '.join(PROJECT_LEADS)}. It is which of the three answers "
                    f"in DESIGN.md 20.1 the project opens with, and the schema's "
                    f"CHECK refuses anything else.",
                )
            # **A road with one stage is not a road.** The blank bundle is
            # allowed one, per 20.4, but a built-in that describes a real
            # process has stretches, and the road strip has nothing to draw
            # without them.
            stages = item.get("stages", [])
            if not isinstance(stages, list) or len(stages) < 2:
                self.fail(
                    where,
                    f"stages is {stages!r}. A built-in process needs at least two, "
                    f"or the road strip has a single waypoint and says nothing.",
                )
            if len(set(stages)) != len(stages):
                self.fail(where, f"two stages share a name: {stages!r}")
            kinds = item.get("date_kinds", [])
            if not isinstance(kinds, list) or not kinds:
                self.fail(
                    where,
                    "date_kinds is empty, so recording a date on this project "
                    "offers no chips at all.",
                )
            for entry in item.get("roles", []):
                if not isinstance(entry, dict) or "id" not in entry or "label" not in entry:
                    self.fail(where, f"roles entry is not an object with id and label: {entry!r}")
            # A rights_note states that a federal rule exists. That is a fact and
            # is fine. It must not expand into a claim about what someone is
            # entitled to in their own state.
            rights = item.get("rights_note", "")
            if rights and re.search(r"\byou (?:are entitled|have (?:a|the) right)\b", rights, re.IGNORECASE):
                self.fail(where, "rights_note states an entitlement rather than that a rule exists")
            self.content_safety(where, item)

    def progress_and_instructions(self):
        path = DATA / "progress-and-instructions.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        self.shared_header(path.name, data, "progress_presets_and_standing_instructions")

        presets = data.get("progress_presets", [])
        if len(presets) != EXPECTED_COUNTS["progress_presets"]:
            self.fail(path.name, f"has {len(presets)} progress presets, expected {EXPECTED_COUNTS['progress_presets']}")
        self.ids_unique("progress_presets", presets)
        for item in presets:
            where = f"progress_presets:{item.get('id', '?')}"
            self.required(where, item, PRESET_REQUIRED)
            if item.get("style") not in VALID_STYLES:
                self.fail(where, f"style is {item.get('style')!r}, not one of {sorted(VALID_STYLES)}")
            if item.get("gap_tolerance") not in VALID_GAP_TOLERANCE:
                self.fail(where, f"gap_tolerance is {item.get('gap_tolerance')!r}")
            if item.get("advice_risk") not in VALID_ADVICE_RISK:
                self.fail(where, f"advice_risk is {item.get('advice_risk')!r}")
            if not isinstance(item.get("medication_markers"), bool):
                self.fail(where, "medication_markers must be a boolean")
            if not isinstance(item.get("unit_options"), list):
                self.fail(where, "unit_options must be a list, empty meaning notes only")
            # Judgment language is checked on every preset, and a high risk
            # preset is where it would appear first.
            self.content_safety(where, item, judgment=True)

        tags = data.get("standing_instruction_tags", {})
        for tag in VALID_TAGS:
            entry = tags.get(tag)
            if not isinstance(entry, dict):
                self.fail(path.name, f"standing_instruction_tags is missing {tag!r}")
                continue
            for field in ("label", "explainer"):
                if not entry.get(field):
                    self.fail(f"standing_instruction_tags:{tag}", f"missing {field!r}")
        federal = (tags.get("federal") or {}).get("explainer", "")
        if federal and not re.search(r"nursing home", federal, re.IGNORECASE):
            self.fail(
                "standing_instruction_tags:federal",
                "the explainer must name nursing homes, because the backing does not "
                "carry over to assisted living, home care, or hospitals",
            )

        instructions = data.get("standing_instructions", [])
        if len(instructions) != EXPECTED_COUNTS["standing_instructions"]:
            self.fail(path.name, f"has {len(instructions)} standing instructions, expected {EXPECTED_COUNTS['standing_instructions']}")
        self.ids_unique("standing_instructions", instructions)
        for item in instructions:
            where = f"standing_instructions:{item.get('id', '?')}"
            self.required(where, item, INSTRUCTION_REQUIRED)
            if item.get("tag") not in VALID_TAGS:
                self.fail(where, f"tag is {item.get('tag')!r}, expected federal or request")
            basis = item.get("basis", "")
            if item.get("tag") == "federal" and basis and not re.search(
                r"nursing home|medicare|medicaid", basis, re.IGNORECASE
            ):
                self.fail(where, "tagged federal but the basis does not name the scope the rule actually covers")
            self.content_safety(where, item)

    def run(self):
        self.situations()
        self.projects()
        self.progress_and_instructions()

        total = sum(EXPECTED_COUNTS.values())
        if self.failures:
            print(f"Template check failed. {len(self.failures)} problems.\n")
            for failure in self.failures:
                print(f"  {failure}")
            print("\nSee templates/SCHEMA.md and templates/README.md for the rules.")
            return 1

        print(
            f"Template check passed. {total} templates "
            f"({', '.join(f'{v} {k}' for k, v in EXPECTED_COUNTS.items())}), "
            f"{self.checked} strings checked for volatile facts, advice, and judgments."
        )
        return 0


if __name__ == "__main__":
    sys.exit(Validator().run())
