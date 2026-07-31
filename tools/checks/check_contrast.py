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

Exit 0 when every pair clears its floor, 1 with the failures otherwise.

Kamsiob, AGPL-3.0.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
COLOR_KT = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui/theme/Color.kt"

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
        ("ink2", "paper", TEXT_FLOOR, "secondary text on the app background"),
        ("ink2", "card", TEXT_FLOOR, "secondary text on a card"),
        ("ink2", "sand", TEXT_FLOOR, "secondary text on a recessed surface"),
        # The corrected tertiary text from DESIGN.md section 2.3.
        ("ink3Text", "paper", TEXT_FLOOR, "tertiary text on the app background"),
        ("ink3Text", "card", TEXT_FLOOR, "tertiary text on a card"),
        ("ink3Text", "sand", TEXT_FLOOR, "tertiary text on a recessed surface"),
        # The single accent, as a link and as a filled button.
        ("blue", "paper", TEXT_FLOOR, "a link on the app background"),
        ("blue", "card", TEXT_FLOOR, "a link on a card"),
        ("onBlue", "blue", TEXT_FLOOR, "the label on a filled button"),
        ("blueDeep", "blueSoft", TEXT_FLOOR, "text in a blue tonal chip"),
        # Gold text is a separate token from blaze, which never renders text.
        ("blazeText", "paper", TEXT_FLOOR, "gold text on the app background"),
        ("blazeText", "card", TEXT_FLOOR, "gold text on a card"),
        ("blazeText", "blazeSoft", TEXT_FLOOR, "gold text in a gold tonal chip"),
        # Green means resolved, and leafText is its text form.
        ("leafText", "paper", TEXT_FLOOR, "green text on the app background"),
        ("leafText", "card", TEXT_FLOOR, "green text on a card"),
        ("leafText", "leafSoft", TEXT_FLOOR, "green text in a green tonal chip"),
        # Red belongs to the emergency card and the open state.
        ("alertText", "paper", TEXT_FLOOR, "alert text on the app background"),
        ("alertText", "card", TEXT_FLOOR, "alert text on a card"),
        ("alertText", "alertSoft", TEXT_FLOOR, "alert text in a red tonal pill"),
        ("onAlertFill", "alertFill", TEXT_FLOOR, "the emergency card header text"),
        # Controls and meaningful graphics. 3:1, per WCAG 1.4.11.
        ("onBlaze", "blaze", UI_FLOOR, "the plus glyph on the capture button"),
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
        # its nodes look like a counterexample, since a node's color carries the
        # entry type, but DESIGN.md section 2.2 requires that color is never the
        # only carrier of meaning, so the type is always stated in the entry
        # beside it. The same holds for a care thread route, which always sits
        # with the thread's name.
        #
        # Holding decoration to a text floor would force the trail to stop being
        # gold, and gold is the whole metaphor. Holding it to nothing and not
        # measuring it would be how the app slowly becomes unreadable. So it is
        # measured, printed, and reviewed by eye on a device.
        ("ink3NonText", "paper", None, "a hairline rule on the app background"),
        ("ink3NonText", "card", None, "a hairline rule on a card"),
        ("blaze", "paper", None, "the trail line and the mark on the app background"),
        ("blaze", "card", None, "a timeline node on a card"),
    ]
    for index in range(4):
        checks.append(
            (f"threadRoute{index}", "paper", None, f"care thread route {index} on the app background")
        )
        checks.append(
            (f"threadRoute{index}", "card", None, f"care thread route {index} on a card")
        )
    return checks


def main():
    if not COLOR_KT.is_file():
        print(f"Contrast check skipped: {COLOR_KT.relative_to(ROOT)} does not exist yet.")
        return 0

    text = COLOR_KT.read_text(encoding="utf-8")
    themes = {"light": parse_theme(text, "LightColors"), "dark": parse_theme(text, "DarkColors")}

    failures = []
    measured = []

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
            "\nDESIGN.md section 2.3 and section 9. Text under 18sp needs 4.5:1, "
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
