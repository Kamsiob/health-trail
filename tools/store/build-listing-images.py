#!/usr/bin/env python3
"""Builds the Play Store listing images from real device screenshots.

**Framed, titled, and on the app's own identity rather than a generic template.**
The eyebrow, the title, the subtitle and the palette are the same hierarchy the
app itself uses, per `CLAUDE.md` rule 15: a quiet mono eyebrow over a lead over
body text. The mark is the launcher's own blaze, drawn from the same geometry as
`ic_launcher_foreground.xml` rather than traced by eye.

**The status bar is already gone** before this runs: `tools/screenshot.sh` crops
132px of it at capture. This additionally crops the gesture pill at the foot, so
no system chrome survives into the listing.

Rule 4 applies to store text: no em dashes. Rule 5: American English.

Usage:  python3 tools/store/build-listing-images.py
Output: docs/store/*.png

Kamsiob, AGPL-3.0.
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SHOTS = os.path.join(ROOT, "docs", "screenshots")
OUT = os.path.join(ROOT, "docs", "store")
FONTS = os.path.join(ROOT, "android", "app", "src", "main", "res", "font")

# The app's own palette, read off ui/theme/Color.kt rather than picked.
INK = (35, 50, 64)
INK_DEEP = (20, 28, 35)
PAPER = (251, 250, 248)
MUTED = (175, 188, 197)
GOLD = (217, 157, 43)

W, H = 1080, 1920

BOLD = os.path.join(FONTS, "roboto_bold.ttf")
REG = os.path.join(FONTS, "roboto_regular.ttf")
MONO = os.path.join(FONTS, "jetbrains_mono_regular.ttf")

# Eyebrow, title, subtitle. Every line is a claim about record keeping and none
# is a claim about care, per rule 2.
PANELS = [
    ("store-today-light.png", "TODAY",
     "The day, before it starts",
     "What is coming, what is still open, and what is waiting to be filed."),
    ("store-notebook-light.png", "THE NOTEBOOK",
     "Twelve places that never move",
     "Care team, medications, papers, money. Always where you left them."),
    ("store-prep-light.png", "APPOINTMENTS",
     "Walk in with your questions ready",
     "Everything you meant to ask, waiting on the visit it belongs to."),
    ("store-medications-light.png", "MEDICATIONS",
     "What they take, and what changed",
     "Every dose change kept with the date it happened."),
    ("store-trail-light.png", "THE TRAIL",
     "Years, in the order they happened",
     "Not the order you found time to type them."),
    ("store-search-light.png", "SEARCH",
     "Find it by the one word you remember",
     "A name, a ward, a word from the note."),
    ("store-projects-light.png", "PROJECTS",
     "The long processes, tracked",
     "An appeal, an application, a records request, and who has it now."),
    ("store-more-light.png", "PRIVATE BY CONSTRUCTION",
     "It never leaves your phone",
     "No account and no cloud. An encrypted file is the only way out."),
]


def gradient(w, h, top, bottom):
    """A quiet vertical wash. Flat would read as a placeholder at this size."""
    base = Image.new("RGB", (1, h))
    px = base.load()
    for y in range(h):
        t = y / max(1, h - 1)
        px[0, y] = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    return base.resize((w, h), Image.BILINEAR)


def rounded(im, radius):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, im.size[0] - 1, im.size[1] - 1],
                                           radius=radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def tracked(draw, xy, text, font, fill, spacing):
    """Letterspacing, which PIL has no notion of and a mono eyebrow needs."""
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill)
        x += draw.textlength(ch, font=font) + spacing
    return x


def wrap(draw, text, font, limit):
    words, lines, line = text.split(), [], ""
    for w_ in words:
        trial = (line + " " + w_).strip()
        if draw.textlength(trial, font=font) <= limit:
            line = trial
        else:
            if line:
                lines.append(line)
            line = w_
    if line:
        lines.append(line)
    return lines


def panel(shot_name, eyebrow, title, subtitle, out_name):
    canvas = gradient(W, H, INK, INK_DEEP).convert("RGBA")
    draw = ImageDraw.Draw(canvas)

    f_eye = ImageFont.truetype(MONO, 26)
    f_title = ImageFont.truetype(BOLD, 62)
    f_sub = ImageFont.truetype(REG, 33)

    margin = 96
    y = 118
    tracked(draw, (margin, y), eyebrow, f_eye, GOLD, 3.0)
    y += 62

    for line in wrap(draw, title, f_title, W - margin * 2):
        draw.text((margin, y), line, font=f_title, fill=PAPER)
        y += 74
    y += 10

    for line in wrap(draw, subtitle, f_sub, W - margin * 2 - 40):
        draw.text((margin, y), line, font=f_sub, fill=MUTED)
        y += 44

    # The device. Everything above sets where it starts, so a two line title
    # never collides with it.
    top = max(y + 54, 430)
    shot = Image.open(os.path.join(SHOTS, shot_name)).convert("RGB")
    # **The gesture pill goes.** The status bar was already cropped at capture;
    # this takes the only other piece of system chrome in the frame.
    shot = shot.crop((0, 0, shot.width, shot.height - 46))

    avail_h = H - top - 64
    scale = min(avail_h / shot.height, 760 / shot.width)
    dev = shot.resize((round(shot.width * scale), round(shot.height * scale)), Image.LANCZOS)
    dev = rounded(dev, 26)

    x = (W - dev.width) // 2
    shadow = Image.new("RGBA", (dev.width + 120, dev.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [60, 60, 60 + dev.width, 60 + dev.height], radius=26, fill=(0, 0, 0, 120))
    shadow = shadow.filter(ImageFilter.GaussianBlur(26))
    canvas.alpha_composite(shadow, (x - 60, top - 52))
    canvas.alpha_composite(dev, (x, top))

    # A hairline so the paper screenshot does not float unattached on the ink.
    ImageDraw.Draw(canvas).rounded_rectangle(
        [x, top, x + dev.width - 1, top + dev.height - 1],
        radius=26, outline=(255, 255, 255, 40), width=2)

    canvas.convert("RGB").save(os.path.join(OUT, out_name), "PNG", optimize=True)
    return out_name


def blaze(size, color):
    """The launcher mark, from ic_launcher_foreground.xml's own geometry.

    Two stacked rounded bars on a 108 viewport: x 25 to 83, heights 15.82,
    at y 31.59 and y 60.59. Drawn rather than traced.
    """
    s = size / 108.0
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for y0 in (31.59, 60.59):
        d.rounded_rectangle(
            [25 * s, y0 * s, 83 * s, (y0 + 15.82) * s],
            radius=(15.82 * s) / 2, fill=color)
    return im


def feature_graphic():
    """1024x500, which is the one size Play accepts for this slot."""
    w, h = 1024, 500
    canvas = gradient(w, h, INK, INK_DEEP).convert("RGBA")
    draw = ImageDraw.Draw(canvas)

    mark = blaze(190, GOLD)
    canvas.alpha_composite(mark, (78, (h - 190) // 2 - 16))

    f_name = ImageFont.truetype(BOLD, 76)
    f_tag = ImageFont.truetype(REG, 30)
    f_eye = ImageFont.truetype(MONO, 22)

    x = 300
    tracked(draw, (x, 168), "BY KAMSIOB", f_eye, GOLD, 2.6)
    draw.text((x, 200), "Health Trail", font=f_name, fill=PAPER)
    draw.text((x, 296), "A private care notebook for family caregivers.",
              font=f_tag, fill=MUTED)
    draw.text((x, 336), "Everything stays on your phone.", font=f_tag, fill=MUTED)

    canvas.convert("RGB").save(os.path.join(OUT, "feature-graphic.png"), "PNG", optimize=True)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for i, (shot, eye, title, sub) in enumerate(PANELS, start=1):
        name = panel(shot, eye, title, sub, f"phone-{i:02d}.png")
        print("wrote", name, "|", title)
    feature_graphic()
    print("wrote feature-graphic.png")
