#!/usr/bin/env python3
"""Measure every color pair in both themes against the WCAG AA floors.

DESIGN.md section 2.3 corrects three colors the mockups used as small text that
do not meet AA at real text sizes, and then says something important about its
own numbers: they are calculated rather than measured, and the measurement is
what counts.

This is that measurement. It is written as a check rather than as a one time
exercise, because a ratio recorded in a document is a ratio that silently stops
being true the first time somebody adjusts a token.

The floors, from DESIGN.md section 2.3 and section 9:

  4.5:1  text under 18sp
  3.0:1  text at 18sp and above, and UI component boundaries

The pairs are read from the Kotlin theme rather than from a copy kept here, so
a token that changes in one place changes here too.

**Two files, because there are two places a pair gets made.** `Color.kt` holds
this app's own tokens, and every screen it draws itself reads those. `Theme.kt`
maps the same tokens onto Material's forty eight color roles, and every
Material component reads the roles instead. Since #385 the interface is built
on Material 3 Expressive, so the second mapping is not a fallback any more, it
is what a button, a chip, a field and the navigation bar actually draw with.

So this also reads `Theme.kt` and checks two things there: that all forty eight
roles are named, since a role left undefined renders in Material's baseline
lavender rather than failing, and that every `on` role clears its floor against
the role it sits on.

Exit 0 when every pair clears its floor, 1 with the failures otherwise.

Kamsiob, AGPL-3.0.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
COLOR_KT = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui/theme/Color.kt"
THEME_KT = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui/theme/Theme.kt"

TEXT_FLOOR = 4.5
LARGE_TEXT_FLOOR = 3.0
UI_FLOOR = 3.0


def channel(value: float) -> float:
    """One sRGB channel, linearized, per the WCAG relative luminance definition."""
    value /= 255.0
    return value / 12.92 if value <= 0.03928 else ((value + 0.055) / 1.055) ** 2.4


def luminance(hex_color: str) -> float:
    hex_color = hex_color.lstrip("#")
    if len(hex_color) == 8:  # AARRGGBB, as Compose writes them
        hex_color = hex_color[2:]
    red, green, blue = (int(hex_color[i:i + 2], 16) for i in (0, 2, 4))
    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)


def ratio(foreground: str, background: str) -> float:
    a, b = luminance(foreground), luminance(background)
    lighter, darker = max(a, b), min(a, b)
    return (lighter + 0.05) / (darker + 0.05)


def parse_theme(text: str, name: str) -> dict:
    """Pull one theme's tokens out of Color.kt."""
    start = text.index(f"val {name} = HealthTrailColors(")
    end = text.index("isDark =", start)
    body = text[start:end]
    tokens = {}
    for match in re.finditer(r"(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)", body):
        tokens[match.group(1)] = match.group(2)
    # Scan to the matching parenthesis rather than the first one. A non-greedy
    # match stops at the ")" closing Color(...), which finds only route zero and
    # then reports the rest as undefined, which looks like a theme problem and
    # is a parser problem.
    marker = "threadRoutes = listOf("
    if marker in body:
        start = body.index(marker) + len(marker)
        depth, end = 1, start
        while end < len(body) and depth:
            if body[end] == "(":
                depth += 1
            elif body[end] == ")":
                depth -= 1
            end += 1
        for index, color in enumerate(re.findall(r"0x([0-9A-Fa-f]{8})", body[start:end])):
            tokens[f"threadRoute{index}"] = color
    # The six tab hues, DESIGN.md 4.3. Each is a TabHue(base, ink, wash) written
    # positionally, so they are read positionally and named here rather than
    # being left as three anonymous colors.
    for match in re.finditer(
        r"(\w+)\s*=\s*TabHue\(\s*Color\(0x([0-9A-Fa-f]{8})\)\s*,\s*"
        r"Color\(0x([0-9A-Fa-f]{8})\)\s*,\s*Color\(0x([0-9A-Fa-f]{8})\)",
        body,
    ):
        name = match.group(1)
        tokens[f"{name}Base"] = match.group(2)
        tokens[f"{name}Ink"] = match.group(3)
        tokens[f"{name}Wash"] = match.group(4)
    return tokens


# (foreground, background, floor, what it is). Every pair the app actually
# renders, named so a failure says which combination is at fault rather than
# which two hex values are.
def pairs_for(theme: dict) -> list:
    checks = [
        # Body text on every surface it lands on.
        ("ink", "paper", TEXT_FLOOR, "primary text on the app background"),
        ("ink", "card", TEXT_FLOOR, "primary text on a card"),
        ("ink", "sand", TEXT_FLOOR, "primary text on a recessed surface"),
        # ink2 is the only other text level there is. DESIGN.md 4.6: at the 4.5:1
        # floor against warm sand there is no room for a third, so the app gets
        # its third level from size and weight instead.
        ("ink2", "paper", TEXT_FLOOR, "secondary text on the app background"),
        ("ink2", "card", TEXT_FLOOR, "secondary text on a card"),
        ("ink2", "sand", TEXT_FLOOR, "secondary text on a recessed surface"),
        # A fold row's label. foldSurface is sand in light and a darker value in
        # dark, so it is measured separately rather than assumed to equal sand.
        ("ink2", "foldSurface", TEXT_FLOOR, "a fold row's label"),
        # A navigation label. navSurface is the one surface deeper than the
        # page, D201, so it is measured rather than assumed to equal sand.
        ("ink2", "navSurface", TEXT_FLOOR, "a navigation label"),
        # The single accent, as a link and as a filled button.
        ("blue", "paper", TEXT_FLOOR, "a link on the app background"),
        ("blue", "card", TEXT_FLOOR, "a link on a card"),
        ("blue", "sand", TEXT_FLOOR, "a link on a recessed surface"),
        ("onBlue", "blue", TEXT_FLOOR, "the label on a filled button"),
        ("blueDeep", "blueWash", TEXT_FLOOR, "text in a blue tonal chip"),
        # Gold text is a separate token from gold, which never renders text.
        ("goldInk", "paper", TEXT_FLOOR, "gold text on the app background"),
        ("goldInk", "card", TEXT_FLOOR, "gold text on a card"),
        ("goldInk", "sand", TEXT_FLOOR, "a mono eyebrow on a recessed surface"),
        ("goldInk", "goldWash", TEXT_FLOOR, "gold text in a gold tonal chip"),
        # Green means resolved, and leafInk is its text form.
        ("leafInk", "paper", TEXT_FLOOR, "green text on the app background"),
        ("leafInk", "card", TEXT_FLOOR, "green text on a card"),
        ("leafInk", "leafWash", TEXT_FLOOR, "green text in a green tonal chip"),
        # Red belongs to the emergency card and the open state.
        ("alertInk", "paper", TEXT_FLOOR, "alert text on the app background"),
        ("alertInk", "card", TEXT_FLOOR, "alert text on a card"),
        ("alertInk", "alertWash", TEXT_FLOOR, "alert text in a red tonal pill"),
        ("blue", "alertWash", TEXT_FLOOR, "the call action on an emergency card row"),
        ("ink", "alertWash", TEXT_FLOOR, "a value on an emergency card row"),
        ("onAlertFill", "alertFill", TEXT_FLOOR, "the emergency card header text"),
        # Controls and meaningful graphics. 3:1, per WCAG 1.4.11.
        ("onGold", "gold", UI_FLOOR, "the plus glyph on the capture button"),
        ("blue", "card", UI_FLOOR, "the focus outline on a card"),
        ("blue", "paper", UI_FLOOR, "the focus outline on the app background"),
        ("leaf", "card", UI_FLOOR, "a resolved indicator on a card"),
        ("alert", "card", UI_FLOOR, "an open incident dot on a card"),

        # Decorative. Measured and reported, but not held to a floor, and the
        # distinction is deliberate rather than convenient.
        #
        # WCAG 1.4.11 covers user interface components and graphical objects
        # required to understand content. A hairline separating two rows is
        # neither: remove it and nothing becomes unreadable. The trail line and
        # its waypoints look like a counterexample, since a waypoint's color
        # carries the entry type, but DESIGN.md 4.4 requires that color is never
        # the only carrier of meaning, so the type is always stated beside it.
        #
        # Holding decoration to a text floor would force the trail to stop being
        # gold, and gold is the whole metaphor. Holding it to nothing and not
        # measuring it would be how the app slowly becomes unreadable. So it is
        # measured, printed, and reviewed by eye on a device.
        ("ink3", "paper", None, "a hairline rule on the app background"),
        ("ink3", "card", None, "a hairline rule on a card"),
        ("gold", "paper", None, "the trail line and the mark on the app background"),
        ("gold", "card", None, "a timeline node on a card"),
    ]
    # The six tab hues, DESIGN.md 4.3.
    #
    # The ink variant carries text and is held to the text floor on every surface
    # it lands on, including its own wash. The base is a shape and is held to the
    # 3:1 control floor. That split is the whole point of the token: measured on
    # adoption, all six bases landed between 3.23:1 and 4.56:1 as small text, and
    # the tab chip is roughly 11sp and is the first element on every section
    # screen. DECISIONS.md D80.
    for hue in ("rose", "teal", "slate", "moss", "manila", "stone"):
        checks.append((f"{hue}Ink", f"{hue}Wash", TEXT_FLOOR, f"{hue} text on its own wash"))
        checks.append((f"{hue}Ink", "paper", TEXT_FLOOR, f"{hue} text on the app background"))
        checks.append((f"{hue}Ink", "card", TEXT_FLOOR, f"{hue} text on a card"))
        checks.append((f"{hue}Ink", "sand", TEXT_FLOOR, f"{hue} text on a recessed surface"))
        checks.append((f"{hue}Base", "paper", UI_FLOOR, f"a {hue} shape on the app background"))
        checks.append((f"{hue}Base", "card", UI_FLOOR, f"a {hue} shape on a card"))
        # The base sits on its own wash too: the tab chip's underline, the
        # avatar's initials field, the icon in its wash. D89 requires each hue to
        # clear the floor against its own wash rather than only against the
        # neutral surfaces.
        checks.append((f"{hue}Base", f"{hue}Wash", UI_FLOOR, f"a {hue} shape on its own wash"))
    for index in range(4):
        checks.append(
            (f"threadRoute{index}", "paper", None, f"care thread route {index} on the app background")
        )
        checks.append(
            (f"threadRoute{index}", "card", None, f"care thread route {index} on a card")
        )
    return checks


def parse_scheme(text: str, tokens: dict, light: dict, is_dark: bool) -> dict:
    """Resolve `Theme.kt`'s Material color scheme into hex values, for one theme.

    **A second place where color pairs are formed, and until #385 nothing
    measured it.** `Color.kt` holds this app's own tokens and everything above
    reads them. `Theme.kt` maps those tokens onto Material's forty eight roles,
    and every Material component draws from the roles rather than from the
    tokens: an `onSecondary` pointed at the wrong token is unreadable text on a
    real screen and is invisible to a check that only reads `Color.kt`.

    The mapping is Kotlin, so this reads the four forms it is allowed to be in:
    a bare token name, `if (isDark) a else b`, `LightColors.token` for a role
    that is fixed across themes, and a literal `Color(0x...)`. A form outside
    those is reported rather than skipped, because a role this cannot read is a
    role nothing is measuring.
    """
    body = text[text.index("ColorScheme("):]
    depth, end = 0, 0
    for index, character in enumerate(body):
        if character == "(":
            depth += 1
        elif character == ")":
            depth -= 1
            if depth == 0:
                end = index
                break
    body = body[len("ColorScheme("):end]
    # Comments carry the reasoning and some of them name tokens. Strip them
    # first, or a sentence explaining a role becomes an assignment.
    body = "\n".join(line.split("//")[0] for line in body.splitlines())

    # The container ladder is computed above the call, since its direction
    # inverts between themes. Resolve those names before the roles that use
    # them.
    locals_ = {}
    for name, when_dark, when_light in re.findall(
        r"val (\w+) = if \(isDark\) (\w+) else (\w+)", text[:text.index("ColorScheme(")]
    ):
        locals_[name] = when_dark if is_dark else when_light
    for name, value in re.findall(
        r"val (\w+) = (\w+)\n", text[:text.index("ColorScheme(")]
    ):
        locals_.setdefault(name, value)

    scheme, unreadable = {}, []
    for role, expression in re.findall(r"(\w+) = ([^,\n]+(?:\n\s*[^,\n]+)*?),\n", body + ",\n"):
        expression = " ".join(expression.split())
        literal = re.fullmatch(r"Color\(0x([0-9A-Fa-f]{8})\)", expression)
        conditional = re.fullmatch(r"if \(isDark\) (\w+) else (\w+)", expression)
        fixed = re.fullmatch(r"LightColors\.(\w+)", expression)
        if literal:
            scheme[role] = literal.group(1)
        elif conditional:
            scheme[role] = tokens[conditional.group(1) if is_dark else conditional.group(2)]
        elif fixed:
            scheme[role] = light[fixed.group(1)]
        elif expression in locals_:
            scheme[role] = tokens[locals_[expression]]
        elif expression in tokens:
            scheme[role] = tokens[expression]
        else:
            unreadable.append(f"{role} = {expression}")
    scheme["_unreadable"] = unreadable
    return scheme


# **Every Material color role there is, and the count is the check.** A role
# left out of `Theme.kt` does not fail to draw, it draws in Material's baseline,
# which is lavender: D167, and how a mockup of this app first came out purple.
# Before #385 the dark scheme named sixteen of these and the light one named
# twenty one, and nothing anywhere noticed.
MATERIAL_ROLES = [
    "primary", "onPrimary", "primaryContainer", "onPrimaryContainer", "inversePrimary",
    "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
    "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
    "background", "onBackground",
    "surface", "onSurface", "surfaceVariant", "onSurfaceVariant", "surfaceTint",
    "inverseSurface", "inverseOnSurface",
    "error", "onError", "errorContainer", "onErrorContainer",
    "outline", "outlineVariant", "scrim",
    "surfaceBright", "surfaceDim",
    "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest",
    "surfaceContainerLow", "surfaceContainerLowest",
    "primaryFixed", "primaryFixedDim", "onPrimaryFixed", "onPrimaryFixedVariant",
    "secondaryFixed", "secondaryFixedDim", "onSecondaryFixed", "onSecondaryFixedVariant",
    "tertiaryFixed", "tertiaryFixedDim", "onTertiaryFixed", "onTertiaryFixedVariant",
]

# Every Material role pair a component actually draws, and the floor it answers
# to. The `on` roles are text and take the text floor. `outline` is this app's
# hairline, which section 4.6 already calls non-text, so it is measured and
# reported rather than held, exactly as `ink3` is above.
MATERIAL_PAIRS = [
    ("onPrimary", "primary", TEXT_FLOOR, "a label on a filled Material button"),
    ("onPrimaryContainer", "primaryContainer", TEXT_FLOOR, "text in a Material primary container"),
    ("onSecondary", "secondary", TEXT_FLOOR, "a label on a Material secondary fill"),
    ("onSecondaryContainer", "secondaryContainer", TEXT_FLOOR,
     "the navigation bar's selected label in its indicator"),
    ("onTertiary", "tertiary", TEXT_FLOOR, "a label on a Material tertiary fill"),
    ("onTertiaryContainer", "tertiaryContainer", TEXT_FLOOR, "text in a Material tertiary container"),
    ("onError", "error", TEXT_FLOOR, "a label on a Material error fill"),
    ("onErrorContainer", "errorContainer", TEXT_FLOOR, "text in a Material error container"),
    ("onBackground", "background", TEXT_FLOOR, "text on the Material background"),
    ("onSurface", "surface", TEXT_FLOOR, "text on a Material surface"),
    ("onSurfaceVariant", "surfaceVariant", TEXT_FLOOR, "supporting text on a Material surface"),
    ("onSurface", "surfaceBright", TEXT_FLOOR, "text on the brightest Material surface"),
    ("onSurface", "surfaceDim", TEXT_FLOOR, "text on the dimmest Material surface"),
    ("inverseOnSurface", "inverseSurface", TEXT_FLOOR, "text on an inverted surface, a snackbar"),
    ("inversePrimary", "inverseSurface", UI_FLOOR, "an action on an inverted surface"),
    ("onPrimaryFixed", "primaryFixed", TEXT_FLOOR, "text on a fixed primary container"),
    ("onPrimaryFixedVariant", "primaryFixed", TEXT_FLOOR, "supporting text on a fixed primary container"),
    ("onSecondaryFixed", "secondaryFixed", TEXT_FLOOR, "text on a fixed secondary container"),
    ("onSecondaryFixedVariant", "secondaryFixed", TEXT_FLOOR,
     "supporting text on a fixed secondary container"),
    ("onTertiaryFixed", "tertiaryFixed", TEXT_FLOOR, "text on a fixed tertiary container"),
    ("onTertiaryFixedVariant", "tertiaryFixed", TEXT_FLOOR,
     "supporting text on a fixed tertiary container"),
    ("surfaceTint", "surface", UI_FLOOR, "the tint a raised Material surface carries"),
    ("outline", "surface", None, "a Material outline on a surface"),
    ("outlineVariant", "surface", None, "a Material divider on a surface"),
]

# The container ladder, which every expressive component sits on. Text lands on
# all five and the ladder is the thing the old scheme had in one theme and not
# the other, so each rung is measured rather than assumed to be near its
# neighbor.
MATERIAL_PAIRS += [
    ("onSurface", rung, TEXT_FLOOR, f"text on {rung}")
    for rung in (
        "surfaceContainerLowest",
        "surfaceContainerLow",
        "surfaceContainer",
        "surfaceContainerHigh",
        "surfaceContainerHighest",
    )
]


def main():
    if not COLOR_KT.is_file():
        print(f"Contrast check skipped: {COLOR_KT.relative_to(ROOT)} does not exist yet.")
        return 0

    text = COLOR_KT.read_text(encoding="utf-8")
    themes = {"light": parse_theme(text, "LightColors"), "dark": parse_theme(text, "DarkColors")}

    failures = []
    measured = []

    if THEME_KT.is_file():
        theme_text = THEME_KT.read_text(encoding="utf-8")
        for theme_name, tokens in themes.items():
            scheme = parse_scheme(theme_text, tokens, themes["light"], theme_name == "dark")
            for role in scheme.pop("_unreadable"):
                failures.append(
                    f"{theme_name}: the Material role `{role}` is written in a form this "
                    f"check cannot read, so nothing is measuring it"
                )
            for role in MATERIAL_ROLES:
                if role not in scheme:
                    failures.append(
                        f"{theme_name}: Theme.kt does not name the Material role `{role}`, "
                        f"so it falls back to Material's baseline lavender. D167"
                    )
            for foreground, background, floor, description in MATERIAL_PAIRS:
                if foreground not in scheme or background not in scheme:
                    failures.append(
                        f"{theme_name}: Material role {foreground} or {background} is not named "
                        f"in Theme.kt, and an unnamed role falls back to baseline lavender"
                    )
                    continue
                value = ratio(scheme[foreground], scheme[background])
                measured.append((theme_name, foreground, background, value, floor, description))
                if floor is not None and value < floor:
                    failures.append(
                        f"{theme_name}: {description}\n"
                        f"      Material {foreground} on {background} measures {value:.2f}:1, "
                        f"floor is {floor}:1"
                    )

    for theme_name, tokens in themes.items():
        for foreground, background, floor, description in pairs_for(tokens):
            if foreground not in tokens or background not in tokens:
                failures.append(
                    f"{theme_name}: {foreground} or {background} is not defined in the theme"
                )
                continue
            value = ratio(tokens[foreground], tokens[background])
            measured.append((theme_name, foreground, background, value, floor, description))
            if floor is not None and value < floor:
                failures.append(
                    f"{theme_name}: {description}\n"
                    f"      {foreground} on {background} measures {value:.2f}:1, "
                    f"floor is {floor}:1"
                )

    if failures:
        print(f"Contrast check failed. {len(failures)} pairs below their floor.\n")
        for failure in failures:
            print(f"  {failure}")
        print(
            "\nDESIGN.md section 4.6 and section 12. Text under 18sp needs 4.5:1, "
            "text at 18sp and above and UI component boundaries need 3:1."
        )
        return 1

    held = [m for m in measured if m[4] is not None]
    decorative = [m for m in measured if m[4] is None]
    print(f"Contrast check passed. {len(measured)} pairs measured across both themes, "
          f"{len(held)} held to a floor and {len(decorative)} decorative.\n")
    for theme_name in themes:
        rows = [m for m in held if m[0] == theme_name]
        tightest = min(rows, key=lambda m: m[3])
        print(f"  {theme_name}: {len(rows)} pairs held to a floor, tightest is "
              f"{tightest[1]} on {tightest[2]} at {tightest[3]:.2f}:1 "
              f"(floor {tightest[4]}) for {tightest[5]}")
    print("\n  Decorative, measured but not held to a floor, lowest first:")
    for theme_name, fg, bg, value, _, description in sorted(decorative, key=lambda m: m[3])[:4]:
        print(f"    {value:.2f}:1  {theme_name}, {description}")

    if "--table" in sys.argv:
        print("\n| Theme | Foreground | Background | Ratio | Floor | What it is |")
        print("|---|---|---|---|---|---|")
        for theme_name, fg, bg, value, floor, description in measured:
            shown = f"{floor}:1" if floor is not None else "decorative"
            print(f"| {theme_name} | `{fg}` | `{bg}` | {value:.2f}:1 | {shown} | {description} |")

    return 0


if __name__ == "__main__":
    sys.exit(main())
