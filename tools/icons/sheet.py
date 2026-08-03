#!/usr/bin/env python3
"""Render every icon the app draws, as one sheet, from the app's own paths.

**It reads `SectionIcon.kt` rather than holding a copy of the drawings.** A
preview that carries its own copy of the paths is a tool reporting on something
other than what it was asked about, which is D66 and D68 and the reason this
file exists at all: an icon sheet that can disagree with the app is worse than
no icon sheet, because it is checked and believed.

The sheet is drawn at three sizes, because an icon is judged at the size it is
used rather than at the size it is drawn:

    44dp   the standard tile in a two column grid, `DESIGN.md` 11.2
    32dp   the compact tile in a three column grid
    20dp   the drawing inside the table of contents row, 5.12

Usage:

    python3 tools/icons/sheet.py [output.png]

Needs ImageMagick, which is what rasterizes the SVG. It writes the SVG beside
the PNG so a failure to rasterize can still be looked at.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = (
    ROOT
    / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui/components/SectionIcon.kt"
)

# The grid and stroke every drawing in this app is authored on, DESIGN.md 5.12.
VIEWPORT = 24.0
STROKE = 1.7

# Enough of the theme to judge a drawing. Light theme, DESIGN.md 2.1.
PAPER = "#FAF6EE"
CARD = "#FFFFFF"
SAND = "#F1EBDC"
INK = "#22384A"
INK2 = "#5A6D7C"


def circle(cx: float, cy: float, r: float) -> str:
    """The same helper `SectionIconPaths` uses, so the sheet draws the same arc."""
    return f"M{cx - r} {cy} a{r} {r} 0 1 0 {r * 2} 0 a{r} {r} 0 1 0 {-r * 2} 0"


def rect(x: float, y: float, w: float, h: float, r: float) -> str:
    """The same helper, character for character in its output."""
    h_run = w - r * 2
    v_run = h - r * 2
    return (
        f"M{x + r} {y} h{h_run} a{r} {r} 0 0 1 {r} {r} v{v_run} "
        f"a{r} {r} 0 0 1 {-r} {r} h{-h_run} a{r} {r} 0 0 1 {-r} {-r} "
        f"v{-v_run} a{r} {r} 0 0 1 {r} {-r} z"
    )


def _kotlin_float(token: str) -> float:
    return float(token.strip().rstrip("f"))


def _branches(body: str) -> list[tuple[str, str]]:
    """Every `X.NAME -> listOf(...)` or `X.NAME -> of(...)` in one `when` body."""
    out: list[tuple[str, str]] = []
    for match in re.finditer(r"(\w+)\.(\w+)\s*->\s*", body):
        name = match.group(2)
        rest = body[match.end() :]
        if rest.startswith("listOf("):
            start = rest.index("(")
        elif rest.startswith("of("):
            start = rest.index("(")
        else:
            continue
        depth = 0
        for i, ch in enumerate(rest[start:], start=start):
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    out.append((name, rest[start : i + 1]))
                    break
    return out


def _paths(arguments: str, sections: dict[str, list[str]]) -> list[str]:
    """One `listOf(...)` argument list, turned into path data."""
    # A branch that simply forwards to another drawing, which is how a capture
    # kind reuses its section's icon. Resolved rather than redrawn, which is the
    # whole point of writing it that way in the Kotlin.
    forward = re.fullmatch(r"\(\s*[\w.]+\.(\w+)\s*\)", arguments.strip())
    if forward:
        return sections[forward.group(1)]

    # Kotlin splits a long path across lines with `+`. Rejoin them, or every
    # fragment would be drawn as a path of its own and the icon would look
    # broken in a way the app it was read from is not.
    arguments = re.sub(r'"\s*\+\s*"', "", arguments)

    out: list[str] = []
    for token in re.finditer(
        r'"([^"]*)"|circle\(([^)]*)\)|rect\(([^)]*)\)', arguments
    ):
        literal, circle_args, rect_args = token.groups()
        if literal is not None:
            out.append(literal)
        elif circle_args is not None:
            out.append(circle(*[_kotlin_float(a) for a in circle_args.split(",")]))
        else:
            out.append(rect(*[_kotlin_float(a) for a in rect_args.split(",")]))
    return out


def read_drawings() -> dict[str, dict[str, list[str]]]:
    """Every drawing in the app, grouped by what it names."""
    source = SOURCE.read_text(encoding="utf-8")

    groups: dict[str, dict[str, list[str]]] = {}
    sections: dict[str, list[str]] = {}

    for signature, title in (
        ("fun of(section: Repository.Section)", "Sections"),
        ("fun of(destination: Destination)", "Destinations"),
        ("fun of(kind: CaptureKind)", "Capture"),
    ):
        start = source.find(signature)
        if start < 0:
            raise SystemExit(f"{SOURCE.name} has no `{signature}`")
        body = source[start : source.find("\n    }", start)]
        drawings = {
            name: _paths(arguments, sections) for name, arguments in _branches(body)
        }
        if title == "Sections":
            sections = drawings
        groups[title] = drawings

    return groups


SIZES = ((72, 44, "judging size"), (44, 26, "44dp tile"), (32, 18, "32dp tile"), (20, 20, "20dp row"))
COLUMNS = 5
CELL_W = 196
CELL_H = 132


def svg(groups: dict[str, dict[str, list[str]]]) -> str:
    rows: list[str] = []
    y = 0
    for title, drawings in groups.items():
        rows.append(
            f'<text x="12" y="{y + 26}" font-family="monospace" font-size="13" '
            f'fill="{INK2}" letter-spacing="1.6">{title.upper()}</text>'
        )
        y += 40
        for index, (name, paths) in enumerate(drawings.items()):
            column, row = index % COLUMNS, index // COLUMNS
            x0 = 12 + column * CELL_W
            y0 = y + row * CELL_H
            rows.append(
                f'<rect x="{x0}" y="{y0}" width="{CELL_W - 8}" height="{CELL_H - 8}" '
                f'rx="14" fill="{CARD}"/>'
            )
            cx = x0 + 8
            for tile, icon, _ in SIZES:
                pad = (tile - icon) / 2
                rows.append(
                    f'<rect x="{cx}" y="{y0 + 10}" width="{tile}" height="{tile}" '
                    f'rx="{tile * 0.28:.1f}" fill="{SAND}"/>'
                )
                scale = icon / VIEWPORT
                body = "".join(f'<path d="{d}"/>' for d in paths)
                rows.append(
                    f'<g transform="translate({cx + pad},{y0 + 10 + pad}) '
                    f'scale({scale})" fill="none" stroke="{INK}" '
                    f'stroke-width="{STROKE}" stroke-linecap="round" '
                    f'stroke-linejoin="round">{body}</g>'
                )
                cx += tile + 8
            rows.append(
                f'<text x="{x0 + 8}" y="{y0 + CELL_H - 22}" font-family="monospace" '
                f'font-size="10" fill="{INK2}">{name.lower()}</text>'
            )
        y += ((len(drawings) + COLUMNS - 1) // COLUMNS) * CELL_H + 16

    width = 12 + COLUMNS * CELL_W + 4
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{y}" '
        f'viewBox="0 0 {width} {y}">'
        f'<rect width="{width}" height="{y}" fill="{PAPER}"/>'
        + "".join(rows)
        + "</svg>"
    )


def main() -> int:
    out = Path(sys.argv[1] if len(sys.argv) > 1 else "icons.png")
    groups = read_drawings()
    total = sum(len(d) for d in groups.values())
    source = out.with_suffix(".svg")
    source.write_text(svg(groups), encoding="utf-8")
    subprocess.run(["magick", str(source), str(out)], check=True)
    print(f"{total} drawings from {SOURCE.name} to {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
