#!/usr/bin/env python3
"""Hold the four locale catalogs to each other, and to the rules about them.

`MASTER_SPEC.md` section 7: every template string must exist in all four locale
catalogs or the build fails, rather than silently falling back to English. A
silent fallback is the worst outcome, because it ships a screen that is half
translated and looks finished.

Exit 0 when clean, 1 with every problem listed otherwise.

Kamsiob, AGPL-3.0.
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
I18N = ROOT / "contract" / "i18n"

SOURCE = "en"
REQUIRED = ["en", "es", "zh", "ar"]

# ICU argument names, so a translation cannot quietly drop one. A missing
# placeholder renders as a sentence with a hole in it.
#
# The identifier must be followed immediately by a comma or a closing brace,
# which is what distinguishes a real argument like {section} or {count, plural
# from the text inside a plural branch like {Nothing yet} or {1 item}. An
# earlier version matched any word after a brace and reported every plural
# branch as a missing placeholder, which is a check crying wolf.
PLACEHOLDER = re.compile(r"\{\s*(\w+)\s*(?:,|\})")

# Three terms must never use their direct cognate, because the cognate misleads
# or stigmatizes. MASTER_SPEC section 7 and templates/README.md.
#
# Checked as literal substrings per locale. This cannot catch every phrasing,
# and it is not meant to: it catches the specific words a translator reaches for
# first, which is where this goes wrong in practice.
BANNED_COGNATES = {
    "es": [
        ("hospicio", "the cognate reads as a poorhouse or an orphanage. Use comfort focused care wording"),
        ("trabajador social", "reads as child protection in several communities. Describe the function"),
        ("trabajadora social", "reads as child protection in several communities. Describe the function"),
        ("poder notarial", "a legal instrument that does not exist in the same form everywhere. Describe what the document does"),
    ],
    "zh": [
        ("临终关怀院", "reads as an institution for the dying. Describe comfort focused care instead"),
        ("социальный", "wrong script, likely a copy paste error"),
    ],
    "ar": [
        ("دار المحتضرين", "reads as a house for the dying. Describe comfort focused care instead"),
    ],
}


# What CLDR requires of a plural in each locale we ship.
#
# **The categories a language actually has, not the ones a translator felt like
# writing.** A string carrying only `other` is well formed ICU and agrees with
# English, which needs only `one` and `other`, so every other check in this file
# passed while Arabic rendered "1 مكالمات", which is "1 calls". Found by reading
# an Arabic screen on the phone; nothing here could see it. #318.
#
# **This is the same shape as `check_string_keys.py`**: two things agreed with
# each other and neither was held to the thing that mattered.
PLURAL_CATEGORIES = {
    "en": {"one", "other"},
    "es": {"one", "other"},
    # Chinese has no grammatical number: `other` alone is correct and complete.
    "zh": {"other"},
    "ar": {"zero", "one", "two", "few", "many", "other"},
}

# The named categories a branch can use. `=0` and friends are explicit values
# rather than categories and are always allowed beside them: this catalog uses
# both and both are right.
PLURAL_BRANCH = re.compile(r"(?<![\w=])\b(zero|one|two|few|many|other)\s*\{")

# Only a value that actually opens a plural is worth looking inside.
IS_PLURAL = re.compile(r"\{\s*\w+\s*,\s*plural\s*,")

# ICU matches an explicit value before a category, so `=0` covers `zero`.
EXPLICIT_ZERO = re.compile(r"=\s*0\s*\{")


def plural_problems(code, catalog):
    """Every plural in one catalog that is missing a category its language needs.

    **Two things are deliberately not reported.**

    A plural whose branches never interpolate the count is a state switch
    wearing plural syntax: "Nothing on it yet" against "Filled in" reads the
    same at one and at seven, and demanding a `one` branch there would force a
    translator to write the same words twice.

    An explicit `=0` satisfies `zero`. ICU matches explicit values before
    categories, so a message that already spells out the empty case does not
    need the category as well, and this catalog uses `=0` widely on purpose.
    """
    found = []
    required = PLURAL_CATEGORIES.get(code)
    if not required:
        return found
    for key, value in sorted(catalog.items()):
        if key.startswith("_") or not isinstance(value, str):
            continue
        if not IS_PLURAL.search(value):
            continue
        # No `#` anywhere means no form varies with the number.
        if "#" not in value:
            continue
        present = set(PLURAL_BRANCH.findall(value))
        if EXPLICIT_ZERO.search(value):
            present.add("zero")
        missing = required - present
        if missing:
            found.append(
                f"{code}.json {key!r} is a plural missing {sorted(missing)}. "
                f"{code} needs {sorted(required)}, so a count in a missing "
                f"category renders in the wrong form."
            )
    return found


def main():
    problems = []

    if not I18N.is_dir():
        print("Locale check skipped: contract/i18n does not exist yet.")
        return 0

    catalogs = {}
    for code in REQUIRED:
        path = I18N / f"{code}.json"
        if not path.is_file():
            problems.append(f"{code}.json is missing. All four locales ship in v1.")
            continue
        try:
            catalogs[code] = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as error:
            problems.append(f"{code}.json is not valid JSON: {error}")

    if SOURCE not in catalogs:
        print("Locale check failed. The source catalog is missing.\n")
        for problem in problems:
            print(f"  {problem}")
        return 1

    def keys(catalog):
        return {k for k in catalog if not k.startswith("_")}

    source_keys = keys(catalogs[SOURCE])

    for code, catalog in catalogs.items():
        if code == SOURCE:
            continue
        missing = source_keys - keys(catalog)
        extra = keys(catalog) - source_keys
        for key in sorted(missing):
            problems.append(
                f"{code}.json is missing {key!r}. A missing key falls back to "
                f"English silently, which ships a half translated screen that "
                f"looks finished."
            )
        for key in sorted(extra):
            problems.append(f"{code}.json has {key!r}, which is not in the source catalog")

    # Placeholders must match, or a sentence renders with a hole in it.
    for code, catalog in catalogs.items():
        if code == SOURCE:
            continue
        for key in sorted(source_keys & keys(catalog)):
            want = set(PLACEHOLDER.findall(str(catalogs[SOURCE][key])))
            got = set(PLACEHOLDER.findall(str(catalog[key])))
            # "plural" and the counter name appear inside ICU syntax in both.
            if want - got:
                problems.append(
                    f"{code}.json {key!r} is missing placeholder(s) "
                    f"{sorted(want - got)}, so the sentence renders with a hole in it"
                )

    # Every plural must carry the categories its own language needs.
    for code, catalog in catalogs.items():
        problems.extend(plural_problems(code, catalog))

    # Direction, since Arabic ships in v1 and the layout depends on it.
    for code, catalog in catalogs.items():
        meta = catalog.get("_meta", {})
        direction = meta.get("direction")
        expected = "rtl" if code == "ar" else "ltr"
        if direction != expected:
            problems.append(f"{code}.json _meta.direction is {direction!r}, expected {expected!r}")

    # The three terms that must not use their cognate.
    for code, rules in BANNED_COGNATES.items():
        catalog = catalogs.get(code)
        if not catalog:
            continue
        for key, value in catalog.items():
            if key.startswith("_"):
                continue
            lowered = str(value).lower()
            for word, why in rules:
                if word.lower() in lowered:
                    problems.append(f"{code}.json {key!r} uses {word!r}: {why}")

    if problems:
        print(f"Locale check failed. {len(problems)} problems.\n")
        for problem in problems:
            print(f"  {problem}")
        print("\nSee MASTER_SPEC.md section 7.")
        return 1

    unreviewed = [
        code for code, catalog in catalogs.items()
        if not catalog.get("_meta", {}).get("reviewed_by_native_speaker")
    ]
    print(
        f"Locale check passed. {len(catalogs)} catalogs, "
        f"{len(source_keys)} keys each, placeholders, plural categories "
        f"and direction consistent."
    )
    if unreviewed:
        # Not a failure. It is a fact the app has to state honestly about
        # itself, per MASTER_SPEC section 7 and open question 6.
        print(
            f"  Not yet reviewed by a native speaker: {', '.join(sorted(unreviewed))}. "
            f"The app says so rather than implying a reviewed translation."
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
