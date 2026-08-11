# DESIGN.md, Health Trail by Kamsiob

This document is the binding source of truth for every visual, motion, and copy decision. Where code and this document disagree, this document wins. It is a living document: any decision made during implementation is written back into it in the same commit.

**There are three visual references, and each governs a stated part of the app.**

| File | Governs | Drawn |
|---|---|---|
| `reference/screen-grid.html` | **Everything, generally.** The identity, the five laws, the six costumes, and every screen not claimed below | Twenty-five screens, 01 through 25 |
| `reference/projects-grid.html` | **The Projects tab**, and every screen, sheet, and component belonging to it | Eighteen screens |
| `reference/today-grid.html` | **The Today tab**, and every screen, sheet, and component belonging to it | Ten screens, plus the card catalog |

**The grid files are the authority on measurement, and this document is the authority on direction.** Spacing, radius, elevation, type scale, and composition are read off the grid for the screen being built. **This document names the tokens and does not restate their values screen by screen**, because a value written in two places drifts in one of them and nobody can tell which is current. Where prose here and a grid disagree on a number, the grid wins and the prose is corrected. D142.

**One exception, and it is not a loophole: a measured accessibility correction supersedes the grid.** The grid's `ink-2` and `blue` measure 4.36:1 and 4.37:1 on `sand`, both under the 4.5 floor, and 4.6 records the corrected values with their measured ratios. **A fidelity pass that "restored" those would be restoring a failure**, so where this document records a correction with a measurement beside it, that value governs and the grid is the stale one. Found on 2026-08-11 while running the first fidelity comparison, which is what such a comparison is for.

**That division is what the fidelity check in 16.6 exists to hold.** A screen built from the prose alone inherits the direction and loses the execution, and it can pass the costume audit, the overflow audit, and the judgment check while not looking like the drawing.

**The two surface grids extend v4; they do not replace it.** The identity, the five laws, the six costumes, the tokens, the interaction grammar, and everything else in this document stand in full and govern both surfaces. **Where the v4 grid drew Today or Projects, those specific drawings are superseded**; every other screen in it is untouched. Adopted 2026-08-04 on the owner's instruction, recorded as D106.

**Sections 20 and 21 encode those two grids in this document's own words**, in full, because a document that points at a file is a document that goes stale beside it. A cold session builds both surfaces from this repository alone.

This document gives the tokens, the real dp and sp values, the dark theme, the measured contrast, and the rules the mockups imply but cannot state.

**`reference/concept-review.pdf` is a historical record of the concept review. It is not the visual reference and its screens no longer describe this app.** It is kept because the sequence and the voice in it are still useful reading. Nothing is built from it.

**Adopted 2026-08-03: design direction v4, which supersedes the direction this document previously carried.** This is a replacement rather than a layer. Where the old direction and this one disagree, this one wins, and the paragraphs describing the replaced patterns were deleted rather than kept as history. What survived is named in section 18. The decision and everything it forced are recorded in `DECISIONS.md` as D76 through D87.

Two things this direction does not override and cannot. `contract/DATA-CONTRACT.md` governs the data. The content rules that keep this app non-medical govern the copy: no advice, no interpretation, no normal or target ranges, no thresholds, no arrows, no color-coded values, no educational content. Any tension between a visual idea and either of those resolves against the visual idea.

---

## 1. The identity

**A field notebook.** The binder a family actually keeps, with colored index tabs, a trail running through it, and a pen always in reach. Paper, ink, tabs, waypoints.

Warm, quiet, legible, unexcited about itself. The person opening this app is exhausted, often standing in a hallway, holding a phone in one hand. Nothing on screen may feel clinical, gamified, or cheerful.

**Every visual decision derives from that object.** When you are unsure how something should look, ask what it would look like in a well-kept binder, then build that from the tokens in section 4 and the inventory in section 7.

**The one bold thing is the trail.** Everything else stays disciplined. If a decoration does not carry information, it does not ship.

---

## 2. The five laws

These govern every screen, including the ones the grid does not draw.

### Law 1. One thing first

Every screen opens with exactly one dominant element, at roughly twice the scale of anything else: the answer to why the person opened this screen. Up to three supporting items follow. Everything else becomes a fold, a sand-colored row naming its contents and their count, opening on a tap. At most one filled action per screen.

**If two things compete for the top, the screen is wrong.**

Folding rather than hiding is the point. Deferred detail speeds people up without costing discovery, because the folded row names its contents and its count. Nothing disappears, it waits. The person in a hallway sees three things. The person with ten minutes opens everything.

**The scale jump is meant to be felt at arm's length.** Hierarchy fails when sizes are close. Hero at 21 to 24sp display, supporting content at 13sp, folded rows at 12sp on sand.

### Law 2. Everything declares what it is

Six costumes, five of them interactive, and everything interactive wears exactly one.

| Costume | What it means | Where |
|---|---|---|
| **Filled pill** | The one primary action on this screen | Blue, or gold only for capture. At most one per screen |
| **Outlined pill** | A smaller action, now, but not the point of the screen | Always a verb or a dialable number |
| **Row ending in a chevron** | Opens another screen | No chevron, no navigation |
| **Bordered chip** | A choice | Outlined when open, filled when chosen. Never shaped like a button |
| **Sand fold row** | Content waiting behind one tap, named and counted | Sand means "more lives here" |
| **Bare** | Just information, and the guarantee that it does nothing | No container, no border, no fill |

**No bare text links anywhere. No long-press-only actions anywhere. Every target 48dp minimum.**

The costume rule is the contract, and section 16 makes it a mechanical audit: any tappable element without a costume, or any costumed element without a handler, fails the screen.

### Law 3. Ask one question at a time

Capture and any multi-field entry is a staged conversation, not a form. One question per stage, three stages maximum: who or what, when, and the note. Progress dots, skip always visible, save always possible, and chips answering most stages in one tap. **The flow is designed to finish in under fifteen seconds standing up.**

**Native before typing.** Voice input is a first-class control on any note stage, sized like it matters, never a footnote. The camera is the first-class answer for anything on paper. The right keyboard for every field type. **Typing is the last resort, not the default.**

**Context pre-answers stages.** Capture opened from a person already knows the person, shown as a chosen chip, skipped, one tap to change. Capture opened from an incident already knows the incident.

### Law 4. Design for year three

A real notebook holds 1,630 entries, 52 questions, 40 documents, and a dozen lab rounds. **Any list that can grow gets four tools the moment it can:**

1. **A sticky context header**, so the person always knows where they are.
2. **An index scrubber on the trailing edge**, for jumping by year and month.
3. **Search scoped to the section**, with one tap to widen.
4. **Pinning.**

**Pinned things live in a marked group at the top of their screen and outrank everything, including the Today hero.** Anything can be pinned: a measure, an entry, a question set, a document, a section. The app proposes an order, the person overrules it, permanently, per thing.

**Closed periods, resolved threads, and finished groups collapse into folds.**

**Layouts are adaptive.** The same screen has an honest shape for empty, one, a few, and many. The grid shows Progress at two sizes, screens 13 and 24, and Questions at its largest, screen 21, as the pattern. One tracked measure gets the whole screen for its chart. Four get a hero and rows. Fifty get pins, groups, and search.

**Sections with volume carry a view toggle** offering comfortable list, compact list, and, where the content is visual, a grid. The default is set per section, and the person's choice is remembered per section. Density is theirs.

**Empty states are warm invitations, never blank.**

### Law 5. The situation shapes the app

The onboarding situation template decides which sections lead and which fold, the default tracked measures, the default capture chips, and what starts pinned. **No two situations open the same app.** A dialysis family sees dialysis sessions on day one and a hospice family never meets a growth chart.

All of it changeable afterward from one screen, without penalty.

---

## 3. Three corrections to the grid file

The grid is the visual reference, and it carries three stale statements from earlier revisions. This document is authoritative on all three, and records the correction rather than the original. Recorded as D77.

1. **Part B's introduction says eighteen screens. Twenty-five are drawn, numbered 01 through 25.** Twenty-five is correct.

2. **Part C's list of undrawn screens is out of date and repeats itself.** It maps Chapters and Appointments as undrawn, but both are drawn, at screens 19 and 22, and the drawn screens win. It also states three mappings twice. Section 14 rebuilds that list clean from the twenty-five screens that actually exist, and keeps it complete from then on.

3. **Part C's heading says depth never exceeds two while its own body correctly describes tab bar, then section, then detail.** Three levels is correct. The rule that matters is section 9's: depth stops at detail, and adding and editing are sheets rather than a fourth level.

---

## 4. Color

### 4.1 The surface ladder

A real ladder, from recessed to raised.

| Token | Hex | Use |
|---|---|---|
| `paper` | `#F6F1E6` | App background. Warm, never white |
| `sand` | `#ECE4D1` | Recessed: inputs, folds, insets, the search bar |
| `card` | `#FFFFFF` | Raised groups and sheets |
| `ink` | `#233240` | Primary text |
| `ink-2` | `#576873` | Secondary text, **and the only other text level there is**, see 4.6 |
| `ink-3` | `#94A0A9` | **Non-text only**, see 4.6. Hairlines, dividers, inactive strokes |
| `hairline` | `rgba(35,50,64,.10)` | Row separators inside a group |
| `hairline-heavy` | `rgba(35,50,64,.20)` | Chip borders, the view toggle's container |

### 4.2 The semantics

Unchanged in meaning and never used for anything else. Each carries a deep or ink variant for text on a wash, and a wash for its own backgrounds.

| Meaning | Base | Text variant | Wash | Used for |
|---|---|---|---|---|
| Action | `blue` `#2E6D8C` | `blue-deep` `#245C77` | `#E2EDF2` | Every action and only actions |
| The trail and capture | `gold` `#D99D2B` | `gold-ink` `#895D10` | `#F5E9CD` | The trail and capture only |
| Resolved | `leaf` `#4E8A5C` | `leaf-ink` `#3B6C48` | `#E2EDE1` | Resolved and done only |
| Attention | `alert` `#B5492E` | `alert-ink` `#9A3C25` | `#F6E2DA` | Emergency, open incidents, disputed |

**Every color that carries meaning carries a word beside it.** An incident is not "the red one," it is the one whose pill says OPEN.

### 4.3 The tab pack

Muted binder hues for section identity, each with a wash and a text-safe ink. **Tabs are identity, never state.**

| Hue | Base, shapes | Ink, text | Wash | Sections |
|---|---|---|---|---|
| `rose` | `#BC6949` | `#995338` | `#F2E1D8` | People, chapters |
| `teal` | `#4D8980` | `#3E6F67` | `#DEEBE6` | Medications, tests, questions |
| `slate` | `#4A5E73` | `#52687F` | `#E3E9F0` | Appointments |
| `moss` | `#484D38` | `#606845` | `#EAECD8` | Progress, care threads |
| `manila` | `#825A17` | `#835E21` | `#F1E6CC` | Documents, money |
| `stone` | `#706A5C` | `#71654B` | `#EAE7E0` | Standing instructions |

**The six are spread across lightness and saturation rather than sitting at the contrast floor**, D89. As the grid drew them they collapsed under red-green color vision deficiency: rose against moss measured 2.4 CIEDE2000 under simulated deuteranopia, which is the same color. **Every hue keeps its angle, which is the owner's mapping; only lightness and saturation move, and they move to separate the six from each other.** They now hold at **11.1 across normal vision, protanopia, and deuteranopia**, and no pair collapses.

**If a later change puts any pair back under about 10, the answer is not more color.** It is the notebook row and the avatar gaining a second distinguisher, the section icon at differing shape weight. D89 holds that in reserve.

**The section-to-hue mapping is an owner decision and is not to be re-derived.** Five come from the grid. **`stone` was added because the grid draws no standing instructions screen and the section needs an identity**, recorded as D79. Any section added later inherits the hue of the section it most resembles in kind, and the choice is recorded here with its reasoning.

**The base hue is for shapes: the tab chip's fill, the avatar circle, the waypoint, the wash band, the icon in its wash. The ink variant is for text.** That split exists because every base hue fails the small-text floor, measured in 4.6. It is the same split this app already uses for gold, leaf, and alert, applied to six more hues rather than a new idea.

**Every section screen opens with its tab chip. Its notebook row carries its icon in its wash.**

**Whole-app surfaces that belong to no section use gold and the base ladder**, never a tab hue: Today, the trail, filing, projects, search, and onboarding.

### 4.4 Color discipline, non negotiable

- **One accent, `blue`.** Every action, every button.
- **`gold` is the trail and capture.** It is not a second accent. It never fills a button that is not the capture button and never colors ordinary text.
- **`alert` never appears as a warning about a measurement, ever**, because the app does not judge measurements.
- **`leaf` means resolved or completed.** Nothing else.
- **A tab hue never means state.** It says which section you are in and nothing more.
- **Color is never the only carrier of meaning**, anywhere.
- **No pure black, and no pure white background.** `card` is white as a surface on warm paper, which is different and intended.

### 4.5 Dark theme

**The derivation stands unchanged from the previous direction and is not re-litigated.** A trail map at dusk, not an inverted document. Surfaces get lighter as they come forward, elevation is carried by surface lightness rather than by shadow, there is no shadow at all, and gold and red keep their exact meanings. **Never black**, which smears on OLED during scroll and is harsh in a dark room, which is when this theme gets used.

**The values were re-derived against the v4 ladder on 2026-08-03**, because the previous ones came from a light ladder that no longer exists, and because the six tab hues had no dark counterpart at all. D87 carries the full derivation and both measured tables.

| Token | Hex | Note |
|---|---|---|
| `paper` | `#141C23` | App background |
| `card` | `#1C262E` | Raised groups and sheets |
| `sand` | `#25313A` | **Recessed reads lighter on dark**, the opposite of light theme, which is correct for dark surfaces |
| `ink` | `#E8EDF1` | 14.60:1 on paper. Never pure white |
| `ink-2` | `#AFBCC5` | 8.87:1 on paper, 6.85:1 on sand |
| `ink-3` | `#6E7C85` | **Non-text only**, as in light |

**The tab pack in dark**, base for shapes, ink for text, wash for its own backgrounds:

| Hue | Base | Ink | Wash | ink on wash | base on paper |
|---|---|---|---|---|---|
| `rose` | `#C79B8A` | `#B98E7E` | `#2F1D16` | 5.53 | 6.95 |
| `teal` | `#A0CFC8` | `#6CADA2` | `#172E2A` | 5.57 | 10.04 |
| `slate` | `#6789AD` | `#829BB5` | `#1B222A` | 5.58 | 4.72 |
| `moss` | `#CFD8B6` | `#9BAA6E` | `#282D18` | 5.66 | 11.60 |
| `manila` | `#D1A761` | `#C39A55` | `#312614` | 5.69 | 7.71 |
| `stone` | `#9F8856` | `#AA976E` | `#2A251B` | 5.34 | 5.02 |

**The six hues are separated along lightness, not only along hue, and that is deliberate.** A first derivation optimized each hue against its own wash alone and produced six colors that **collapsed under red-green color vision deficiency**: rose against stone measured 2.8 under simulated deuteranopia, which is the same color. **Lightness is what survives red-green CVD**, so the hues keep their angles exactly, which is the owner's mapping, and spread across a 48 to 78 percent lightness band. **Minimum pairwise separation is now 10.8 CIEDE2000 across normal vision, protanopia, and deuteranopia.** A wider band scored no better and cost hue identity.

**The tables are the floor, not the verification.** Simulated protanopia and deuteranopia screenshots of the notebook screen at both themes are captured and looked at before the dark theme is called done. **A number saying two colors differ is not a person telling them apart on a phone in a dark room.**

**The capture button stays gold in both themes**, and its glyph is not white. White on gold measures 2.38:1, well under the 3:1 a control needs. The fill stays gold, which is what carries the meaning, and the glyph darkens to `#2B1D06`, which measures 6.88:1. This is the one control the app cannot afford to have anyone miss.

### 4.6 Contrast, measured

**Measured rather than calculated**, by `tools/checks/check_contrast.py`, which reads the tokens out of the theme itself and runs on every push. **Floors: 4.5:1 for text under 18sp, 3:1 for text at 18sp and above and for interface components required to understand content.** These floors stand unchanged from the previous direction and are a gate, not a target.

**The v4 palette was measured on adoption and three classes of correction came out of it.** Every number below is measured against the actual warm surfaces, not against white, which is the mistake that invalidated an earlier pass.

| Problem as drawn | Correction | Measured, at its tightest surface |
|---|---|---|
| `ink-2` `#5A6B77` measures 4.36:1 on `sand` | `#576873` | 4.57:1 on `sand` |
| `blue` `#2F6F8F` measures 4.37:1 on `sand` | `#2E6D8C` | 4.50:1 on `sand` |
| All six tab hues used as small text, 3.23:1 to 4.56:1 | The ink variants in 4.3 | 4.50:1 to 4.79:1, worst case `teal` on `sand` |
| `ink-3` `#94A0A9` at 2.37:1 on paper | **Non-text only.** It never renders text | Not applicable |
| `gold` `#D99D2B` as text at 2.12:1 | `gold-ink` `#895D10`. Gold itself never renders text | 4.56:1 on `sand` |

**There are two text levels in this app, `ink` and `ink-2`, and `ink-3` is non-text only. That is a consequence of the floor rather than a style choice, and a third is not to be reintroduced from the old table.** D92. The previous direction carried a text-safe tertiary alongside `ink-2`. Against warm `sand` at 4.5:1 there is no room for one: anything light enough to read as tertiary fails, and anything that clears the floor is `ink-2` again. **So `ink-3` is non-text only and the third level comes from size and weight**, which is what law 1's scale jump is made of anyway. Every call site that used the old tertiary text token now uses `ink-2`.

**The tab chip is the reason this matters rather than a theoretical concern.** The grid draws it at 8px uppercase mono, which is roughly 11sp at real scale, and it is the first element on every section screen. Drawn in the base hue it fails on all three surfaces. Drawn in the ink variant on its own wash it clears, and it looks the same at arm's length.

**What is decorative, and why that is not a loophole.** A hairline, the dashed trail line, and a waypoint are measured and reported but are not held to 3:1. None is an interface component required to understand content: remove a hairline and nothing becomes unreadable, and a waypoint's color is never the only thing carrying its meaning, because 4.4 requires a word or a shape alongside it. For the record, the gold trail line sits at 2.12:1 on paper and a hairline at 1.26:1. These are reviewed by eye on a device rather than ignored. The alternative would be forcing the trail to stop being gold, and gold is the entire metaphor.

### 4.7 Elevation

Light theme: cards and groups carry a soft warm shadow, two layers, `0 10dp 26dp rgba(60,54,38,.10)` plus `0 2dp 6dp rgba(60,54,38,.06)`, and a smaller `0 2dp 8dp rgba(60,54,38,.08)` for rows and thumbnails. **Never a hard drop shadow, and never a border as a card's only definition.**

Dark theme: no shadow. Elevation is `paper` to `card` to `sand`, plus an optional hairline where two surfaces of the same value must separate.

Print and PDF paths substitute a 1dp hairline, because large soft shadows rasterize as dark smudges. Learned the hard way producing the concept document.

---

## 5. Type

**Three faces, three jobs.** The separation is the system, and mixing them is the most visible way to break it.

| Face | Job |
|---|---|
| **Bricolage Grotesque** | Display, titles, row titles, hero lines. **The only bold voice.** |
| **Atkinson Hyperlegible** | Everything a person reads, including dates, locations, and roles |
| **JetBrains Mono** | **Strictly data**: eyebrow labels, counts, amounts, phone numbers, measurement values, distance markers. Always tabular |

**Mono never touches a date, a location, a role, or anything with a verb.** A date is something a person reads, so it is Atkinson. A count is data, so it is Mono. That line is the one most often crossed and it is what makes a screen read as a dashboard instead of a notebook.

### 5.1 The scale ladder

| Role | Face | Size / line | Weight | Use |
|---|---|---|---|---|
| Hero | Bricolage | 21 to 24sp / 1.18 | 800, tracking -0.015em | The one thing, section 2 law 1 |
| Display M | Bricolage | **22sp** / 1.1, and 19 to 20 where a screen's own title is set smaller | 800, -0.02em | Screen titles, the current chapter, a round's reason |
| Row title | Bricolage | 13sp / 1.3 | 700 | The title line of a group row |
| Body | Atkinson | 13sp / 1.5 | 400 | Supporting content. **The floor** |
| Body S | Atkinson | 12sp / 1.4 | 400 | Folded rows on sand, row subtitles |
| Label | Atkinson | 13sp / 1.4 | 700 | Buttons and chips |
| Nav label | Atkinson | 11sp / 1.3 | 700 | Bottom navigation only. Exempt from the floor |
| Mono | JetBrains Mono | 11sp / 1.5 | 700, tracking 0.12em, uppercase | Eyebrows, counts, dates as data, amounts. Exempt from the floor |
| Mono L | JetBrains Mono | 22 to 30sp | 700, tracking 0, tabular | A number at display size, in the big-number component and nowhere else |
| Display L | Bricolage | 28sp / 1.2 | 800 | **Implemented and not in the grid's `.h1` vocabulary.** The heading of a thing's own screen, above a section title. Checked against a drawing on #345 |
| Display S | Bricolage | 18sp / 1.33 | 800 | **Implemented and not in the grid's `.h1` vocabulary.** A card's own title where a row title is too quiet. Checked against a drawing on #345 |

**This ladder was corrected on 2026-08-11 and the correction is the point of D142.** It said Display M was 19 to 20sp. **The grid's `.h1` is 22px** with per-screen overrides at 19 and 20, and the app has been 22sp all along, so **the prose was the only one of the three that was wrong** and a session building a new screen from this table would have set its title two steps under both. Display L and Display S were missing from it entirely while being used across the app.

**The jump from 21sp to 13sp is large on purpose and is meant to be felt at arm's length.** It is what law 1 is made of. Closing it to make a screen look balanced is the most common way to lose the hierarchy.

**Mono L carries no tracking.** The 0.12em exists to raise letter distinction at 11sp. At 22sp and above the same tracking pulls the digits of one number apart until it reads as several numbers. Tabular figures do the alignment work instead.

**The two exemptions from the 13sp floor, and why they are the only two.** The nav label and the Mono metadata style sit below it. Neither ever carries information on its own: a nav label is always paired with an icon and a content description, and a Mono eyebrow or count is always directly beside the content it labels. Both are short, and Mono is uppercase and tracked, which raises its letter distinction well above 11sp lowercase body text. Both scale with dynamic type. **Nothing else may be added to this list.** A third candidate is a sign the layout is too dense, and the layout gets fixed rather than the floor lowered.

**The nav label is capped at 1.4, and it is the only capped type in the app.** At font scale 2.0 "Notebook" broke mid-word and collided with the capture button. Four labels and a fixed clearance for the FAB share one row, which is a width budget nothing else has. Above the cap the label holds and the icon, the position, and the content description carry it. Found on the phone with the system font at maximum, which is why section 12 requires that pass rather than a reading of the code.

### 5.2 The faces must be verified on the device before any screen work

**This is a gate, not a checkbox.** This app has already shipped multiple sessions of visual review that were invalid because a face had silently fallen back to the system typeface and nobody checked.

Before any screen work begins: screenshot a screen containing all three faces, confirm the letterforms by eye, and **record the confirmation in `HANDOFF.md`**. A review run against a fallback face is not a review.

### 5.3 Script coverage, which is a real problem rather than a detail

**Version one ships English, per D141, and none of this section is deferred by that.** The bundled Arabic face and the CJK reasoning both stay: they are what makes a returning language a content job rather than a typography job, and font fallback defects are exactly the kind that are invisible until somebody's name renders as a box. Removing coverage now to add it back later is the retrofit this section exists to prevent.

Bricolage Grotesque and Atkinson Hyperlegible cover Latin. **Arabic needs Noto Sans Arabic, bundled.** In Arabic, display text uses the Noto face at bold weight rather than forcing a Latin display face that has no coverage. Never ship a screen where one language renders in a different face than the rest of that language's screen.

**Chinese uses the system face and is not bundled. This is a rule, not a compromise.** Android ships Noto Sans CJK and renders it well, and it is the face a Chinese-reading person already sees in every other app on their phone. Bundling would add roughly ten megabytes per weight to reproduce something already present and correct. Arabic is bundled because coverage there is genuinely inconsistent across devices, so the same reasoning gives the opposite answer.

**Never subset a bundled face.** A subset covers the glyphs somebody thought to include, and **the thing most likely to fall outside a subset is a person's name or a facility's name**, which is exactly the content that must never render as a box. The cost is not cosmetic: it is a record that cannot be read back.

**Verified on the device on 2026-08-01 and the result stands.** Every Chinese glyph renders from the system face with no missing-glyph boxes. **The mono eyebrow is served by the system CJK face rather than by JetBrains Mono**, which has no CJK coverage. That substitution is correct and intended rather than a defect. Latin digits inside an otherwise Chinese line keep the mono face and the mixed line reads normally.

**Use `zh-Hans`, never a bare `zh`.** Hans and Hant are different writing systems rather than dialects, and Android's locale negotiation will not guess. Getting it wrong does not produce an error, it produces English.

### 5.4 Right to left

Arabic ships in v1, so every screen is built direction-aware from the first screen. This is not a late localization pass.

- All layout uses start and end, never left and right.
- Directional icons mirror: chevrons, back arrows, the trail's direction of travel.
- **The trail mirrors**, including waypoints and the dashed rail. A timeline reading left to right in an RTL layout is broken, not stylish.
- **The edge scrubber moves to the start edge, and the FAB moves to the start corner.**
- **A thumbnail image does not mirror**, because a photograph of a page is not directional. The grid order and the caption do.
- Numbers, dates, and measurements follow locale conventions, and the mono style is verified to render rather than reverse.
- Every screen is screenshotted in Arabic and compared against its English counterpart.

---

## 6. Shape and rhythm

| Thing | Value |
|---|---|
| Cards and groups | 16 to 18dp radius, **no borders**, soft shadow per 4.7 |
| Chips and pills | Fully rounded |
| Sheet top corners | 24dp |
| Thumbnail | 13dp |
| Avatar | Circular |
| Screen padding | 13dp |
| Group row padding | 11 to 13dp |
| Between stacked elements | 10dp |
| Minimum row height | 46dp visually, **48dp touch target always** |

A row may be 46dp tall and still carry a 48dp touch target through invisible padding. **The touch floor is never traded for density.**

---

## 7. The component inventory

**Compose every screen from these. Do not invent a new one.**

| Component | What it is |
|---|---|
| **Tab chip** | The section's identity, top of every section screen. Ink variant on its own wash, per 4.3 |
| **Grouped surface with hairline rows** | The `card` container holding rows separated by hairlines. No hairline after the last row |
| **Fold row** | Sand, named, counted, opens in place. The workhorse of law 1 |
| **Filled action** | The one primary action. Blue, or gold for capture |
| **Outlined action** | A smaller action. Always a verb or a dialable number |
| **Chip** | A choice. Outlined open, filled chosen |
| **The one-thing hero** | Sits directly on paper. No surface, no shadow, no border. At most one per screen, and none is valid |
| **Today card** | The unit of the Today surface, and **the shape all seventeen types share**: an index tab in the card's own section hue naming where the answer lives, the answer sized by the card, a quiet line of context, and a corner chevron, because every card is a door. Three sizes with a touch target budget, 21.3: one at small, two at wide, three at tall. **Identity comes from the tab pack, never from decoration**, and a card pointing at one thing names it on the tab. **Not for any other screen**: a card here is something the person chose to have and can remove, and wearing the shape where that is not true makes a promise the app cannot keep. **Its one inline action sits outside its answer, and that is structural rather than cosmetic**: the answer is silenced so a reader hears the card as one sentence rather than four, and a control drawn inside that silence is reachable by finger and by nothing else |
| **Today's lead slot** | The hero in Today's costume, and **exactly one, never zero and never two**, per 21.1. Sits directly on paper at display scale with a mono eyebrow in its card's hue, and it is **never a card**: the lead wearing the card costume puts the most important thing on the screen at the weight of the four things under it. Its eyebrow is the day for the digest and the promoted card's own name for anything else. **Not for any screen but Today**, where the same job is the hero's |
| **Wash band** | A section-colored summary strip, for a total or a state at a glance |
| **Road strip** | A project's stages as a horizontal stretch of the trail: waypoints on a dashed rail, **full** on the project's own screen and **mini** on its card. **Bare, and that is the whole of its costume**: it is information, it does nothing on touch, and the way along it is an outlined action beside it, because a tappable waypoint would make an information graphic look like a picker. **Use it for a process with named stages somebody agreed to.** **Not for anything that is not a sequence**, and not for progress through work: it says where a process is, never how far through it somebody is. Mirrors fully in right to left |
| **Standing card** | Whose hands a thing is in and since when: a mono eyebrow, a plain display sentence, the elapsed fact, and outlined actions. **Use it wherever a screen's answer is a state somebody else is holding**, which is where a project stands, how a closed one ended, and what a file held while nobody was looking. **Not for a fact about the record itself**, which is a stat, and **not where there is nothing to act on**, where a line of text is lighter. The sentence itself is bare; only the actions are pills |
| **Date row** | A tabular countdown beside what the date is and where it came from, ending in a chevron. **Use it for one date somebody has to be ready for.** **Not for a list of dates**, which is the agenda list, and **never for a date the person gave coarsely**: a countdown from "sometime in April" is invented precision, and the row shows the date instead |
| **Latest word card** | The most recent thing said, quoted in a gold wash band with who said it and when, opening the entry it quotes. **Gold because it is a trail entry surfaced**, not a new kind of thing. **Use it where the newest entry is itself the answer.** **Not as a feed**: it is one entry, and a second one under it makes it a list that has stopped being a quote |
| **Step row** | A checklist line with an optional handler tag. **The box is the only interactive part** and the tag is data: mono, bare, and never an account. **Use it for work somebody actually has to do.** **Not for a stage**, which is the road, and **not with a completion count above it**, per rule 13: a tally of somebody's own diligence is what this app does not do |
| **Reference line** | A reference number in mono inside a sand pill. **The standard dress for a reference number everywhere in the app**, not only on a project: a claim number, a case number, a confirmation code. **Use it wherever the person will have to read a number back to somebody.** **Not for an ordinary number**, which is text: this costume says "this is the string they will ask you for" |
| **Spine with waypoints** | Filled done, ringed open, hollow upcoming, gold-ringed milestone. Dashed rail, and mono gap markers such as "six days pass" |
| **Avatar** | Initials in the section wash |
| **Avatar overflow** | The last circle in a row of avatars, saying how many are not drawn. **The same circle as an avatar and never a control**: it is the last member of the row rather than a second door, and the card behind it is the door. A row of faces that stops has to say it stopped, or three of nine reads as a care team of three, which is the app deciding which three of somebody's people matter. Quiet ink, so the eye reads the names first. **The plus is bidi isolated**, because a neutral character takes the paragraph direction and "+12" came out "12+" in Arabic, which is a different claim |
| **Stat and big-number** | Tabular mono |
| **Chart card** | Name, latest value, plain polyline with dots, **gaps drawn as gaps**. No target lines, no zones, nothing colored by value |
| **Round card** | For lab work. One card per draw, holding its tests, its reason, its documents, and who ordered it. **Every test name is a door to that test's own history** |
| **Agenda list and month grid** | For appointments, drawn only from what is recorded in the app |
| **Thumbnail gallery** | The person's own paper, visible |
| **Scoped search bar** | Sand, rounded, sitting at the top of the list it searches and **never reaching outside it**. Its hint says how many things are in scope. Filters as you type, with no submit; a clear control appears only once there is something to clear. **Not on a list that fits in a screenful or two** |
| **Universal search door** | The way into search, **fixed under Today's lead and nowhere else in the app**, per 21.1 and `MASTER_SPEC.md` 4.8. A sand pill carrying the same magnifier the field it opens wears, and **it is a button rather than a field**: search is a whole screen with its own field, and a second live field here would be two places to type one query with one of them throwing the words away. Finding and recording are the two acts that must never move, so this and the gold capture button stay put while everything between them is the person's to arrange. **Not inside a section**, which searches itself with the scoped search bar |
| **View toggle** | Remembered per section |
| **Pin marker and pinned group** | The person's own hierarchy, above everything |
| **Sticky section header** | So the person always knows where they are |
| **Edge scrubber** | Jump by year and month, riding **a reserved margin rather than sitting over the words**, in a band of roughly half the screen height. Every label is also its own tap target, so the gesture is never the only way. **Landing in a closed fold opens it**, because answering a jump with a door is not answering it. Same "not on a short list" rule as the search bar |
| **Stage dots** | Where somebody is in a staged conversation, per law 3. Two to five stages. The current dot is **wider**, never a different color alone, and the row is **one node to a reader** saying which stage of how many in words. **Never a progress meter**: it says where you are and never how much is left to do, because rule 13 rules out grading the person's own diligence. Not for a screen with sections, which is not a sequence somebody agreed to |
| **Sheet** | Rises from the bottom, 24dp top corners, scrim behind. **One question per sheet**, per law 3, and it says whether anything it does is saved. **It scrolls**, because a sheet that cannot reach its own Done at font scale 2.0 is one somebody is stuck in |
| **FAB** | Gold, 48dp, bottom-trailing corner, opening a labeled menu that blooms upward from it |
| **Bottom navigation** | Four tabs, even spacing, active tab marked with a tonal pill behind its icon |
| **Waiting** | One quiet word, centered, while something is read. **No spinner, no progress bar, no skeleton rows**: a bar would claim to know how long a database read takes, and skeleton rows draw a shape the real answer may not have, so an empty section would flash three gray rows and then show nothing, which reads as something having been lost. **One treatment for the whole app**, because there were two, six identical lines apart, which is how loading states drift |

**Any component in the current codebase that is not on this list is retired.** Do not keep a one-off around because a single screen uses it. Rebuild that screen from the inventory. The retirements are enumerated in `DECISIONS.md` D82.

**A component is not defined until it says when to use it and when not to.** That was the omission that let one card shape spread across twenty screens. Each entry above carries its "what it is for," and the choosing rule is law 2 read from the other direction: pick the costume from what the person is doing, and the component follows.

**Not everything is a card, and making everything a card is why nothing stands out.** A tile for a fixed set of destinations. A dense row for a long list somebody scans. A card only for something with three or more lines they actually read. One hero per screen at most. A spine for anything sequential. A thumbnail where the app holds the person's own paper. An avatar for a person.

**A screen also has a layout pattern**, and using the same one as the screen it was reached from is a deliberate choice rather than the default.

**A dense row's second line is one line when it is a tag and uncapped when it is a sentence.** A role, a state, a category or a date is one line by nature and stays there, which is what lets fifty rows be scanned. Where the second line is what somebody actually reads to choose, it wraps freely: **a fixed cap is a cap at the smallest type size and a truncation at the largest**, so two lines looked like a fix at scale 1.0 and clipped again at 2.0. D105.

---

## 8. The FAB correction

**In the grid, some bottom-anchored full-width buttons run underneath the corner FAB.** That is an error in the drawing, corrected here as it is adopted. Recorded as D81.

**On any screen where the FAB is present:**

1. A bottom-anchored action bar **does not span the full width.** It ends before the FAB's zone, leaving the FAB's width plus a 12dp gap clear on the trailing side, so button and FAB sit side by side without overlap.
2. **Scrolling content gets enough bottom padding that the last item can scroll fully clear of the FAB.**
3. **Nothing tappable ever sits underneath the FAB.**

This is checked mechanically by the overflow audit in section 16, at both font scales, in both directions. In RTL the FAB is in the start corner and the clearance moves with it.

---

## 9. The interaction grammar

**Depth is tab bar, then section, then detail, and stops.** Adding and editing are **sheets over where you stand, never a fourth level.**

**A sheet holds one question, which is what decides sheet or screen.** Section 7 already says it of the component: one question per sheet, per law 3. So recording how somebody answered, moving a project along its road, writing down a date, logging a call and editing one step are sheets, and **a form with several fields is a screen at the same depth**, not a fourth level and not a sheet somebody cannot reach the bottom of. Adding a person, a medication, an appointment, a bill, a document, a standing instruction or a milestone are screens for that reason: at font scale 2.0 a sheet holding eight fields is one the person is stuck in, which is the failure section 7 names when it says a sheet scrolls.

**Folds open in place** rather than on a new screen, unless what they hold exceeds a screenful.

**Every list ends in an add affordance in context**, prefilled with the context it was opened from, and the global gold capture exists everywhere besides it.

**Every detail screen has one outlined Edit.** A single field shows Change beside it.

**Removal is reached from the thing itself, never from a row in a list.** An outlined "Remove this" at the foot of the thing's own screen, or in the sheet its row opens where it has no screen, and it opens the confirmation rather than removing anything. **So there is still no destructive control resting on any list**, which is 5.4's requirement, and there is no capability that only somebody who already knew the gesture could find, which is 13.5's. `DECISIONS.md` D135, and it is retroactive.

**In content, an action is a pill sized to its label, and that includes a sheet.** Full width outlined is what the scaffold uses at the foot of every screen to mean the way back, so a second and a third one above it make a column of identical bars of which only the last leaves. **A sheet has the same problem wearing a different name**: drawn full width under Cancel, a removal is the control that leaves and the control that removes in one costume, a thumb's width apart. D118, corrected by looking at the sheet.

**Tapping a row opens it.** Selection states exist only inside sheets. **Any swipe action also has a visible button path.**

**Every save button takes what is there.** Partial is a finished state. Empty reads as not yet and never as an error. **Drafts survive leaving the screen.**

**Everything named on a screen is a door, and the other side shows the way back.** If A shows B, then B shows A. A one-way link is a dead end wearing a disguise. Carry context forward instead of asking again, and count the taps: a flow that takes four when it could take two gets abandoned in a hallway.

### 9.1 Appointments stay offline by construction

The agenda and month views are **drawn only from appointments recorded in this app.** No account, no sync, and **no calendar read permission is ever requested.**

A single appointment offers to hand itself to the phone's own calendar app: one event, user-initiated, one way, **with nothing read back.**

**What travels is what makes an event, and nothing else: the name, the day, and where.** The notes stay in the notebook. A calendar is, on most phones, the one thing on the device that syncs to an account by default, and the notes on an appointment are the care record. Rule 23 filters on safe, private, and compatible before it asks what is easiest.

**The offer only appears where it can be kept.** A date coarser than a day is not an event, and handing "sometime in March" over as March 1st would invent a precision nobody gave. A phone with nothing that can take an event shows no action at all rather than one that fails when tapped.

**`ACTION_INSERT` writes nothing.** It opens the calendar app's own new-event screen already filled in, and nothing exists until the person saves it there, so they see exactly what is going across before it goes.

### 9.2 Dates

The storage model is `contract/DATA-CONTRACT.md`, and the whole point is that none of it reaches the screen.

**The person never sees EDTF, never sees a precision selector, never chooses a storage format.** They see chips for the common cases, an exact date and time always available without leaving the flow, and natural expression where it is easier. **The exact date and time is a peer of the chips, not something behind them.**

**Whatever the person expresses is recorded at exactly that precision and no finer.** A month stays a month.

**Display never invents precision.** "Sometime in November 2024" is honest. "November 1, 2024" for that same input is a fabrication. This holds in the trail, in exports, in PDFs, and in the engine's composed sentences.

**Unknown is a first-class value.** An entry with an unknown date saves, is valid, and appears in the trail. Never blocked, never hidden, never quietly filled in with today.

**Every date is editable forever, from the entry itself, with the same control.** Editing a date never creates a new entry and never loses the entry's links.

**Imprecise entries never disappear.** They sort sensibly among precise ones and appear in a date-range search whenever their range overlaps the query.

**Charts plot an imprecise date honestly or show a gap**, never interpolated and never presented as more certain than it is.

---

## 10. Motion

Two spring personalities and three durations.

**Standard spring**, everything by default: stiffness 380, damping ratio 0.9, no overshoot. Screen transitions, sheet presentation, list item entry, expansion.

**Expressive spring**, slight overshoot, stiffness 300, damping ratio 0.68. **Reserved for exactly three moments and nothing else:** the capture menu blooming, a milestone landing on the arc, and an incident marked resolved. Three moments, because each is a small piece of relief in an app used during hard times.

**Durations:** quick 120ms for press feedback and chip selection, standard 240ms for sheets and navigation, deliberate 400ms for the trail drawing itself in on first view.

**Reduced motion, when the system setting is on**, and this rule stands unchanged from the previous direction: every spring becomes an instant state change, the trail draw becomes an immediate render, and the only remaining transition is a 100ms opacity fade. **Verify by actually enabling the setting, not by reading the code.**

**Motion carries meaning here or it does not ship.** Identical fade-ins applied to everything read as generated and are banned in section 17. Every spec comes from the tokens through `LocalMotion`, **never built inline**, because a spec built inline is one the reduced motion setting cannot reach.

---

## 11. Voice

Write like a person explaining something to a friend across a table. Plain words, short sentences, contractions welcome.

**Never**: exclamation points, hype words, fear language, apologies for the software's own limits, sentences that could appear in a generic tech advertisement, or anything that congratulates the person for using the app.

**No em dashes** in anything a person reads: app copy, documentation, README, commit messages, store text. Commas, periods, and colons instead. Source code is exempt where the character is functionally required.

- **Never imply a lapse is a failure.** "Since you were last here" is correct. "You have not logged anything in 3 weeks" is banned. No streaks, no completion percentages on the person's own diligence, no catch-up prompts, no reminders to use the app.
- **Never interpret.** The app says what it counted and stops. "This suggests a pattern of neglect" is banned in every form.
- **Never advise.** No medical guidance, no legal guidance, no "you should," no "consider asking." Templates offer administrative actions, phrased as things families do, never as recommendations.
- **Honest limits, at the moment they matter.** The medications screen says plainly that the app keeps the record and does not remind or alert. Say it where the person is, not buried in a settings page.
- **Second person, warm, never familiar.** "Write down the direct number for the unit." Not "Let's write down" and not "Don't forget to."
- **Spanish uses formal usted throughout**, and elders are addressed as Señor or Señora.
- **Three terms must never be translated with their direct cognate**, because the cognate misleads or stigmatizes: hospice, power of attorney, and social worker. Use descriptive phrasing plus a plain gloss carried in the template data's `localization_note` fields.
- **Failure speaks plainly.** Name the file, name what was wrong with it, say what the person can do. Never a stack trace, never an error code alone, **never a message implying the person did something wrong.**
- **Snackbars confirm in the same words as the button that caused them.**

### 11.1 The disclaimer, final wording

Shown on first launch, before any part of the app is usable, with an explicit accept. **The safety substance may not be cut on the grounds of warmth.**

> **Before you start**
>
> Health Trail is a notebook. It helps you keep track of someone's care: the calls, the visits, the questions, and the paperwork that piles up around all of it.
>
> **It is not a medical app**
>
> It gives no medical advice, and it is not a medical device. Nothing here replaces a doctor, a nurse, emergency services, or advice from a lawyer. If something is urgent, call emergency services.
>
> **What you write stays on this phone**
>
> There is no account and no cloud. Your notes live on this device and go nowhere else unless you send them somewhere yourself.
>
> **The record is yours**
>
> The app writes down what you tell it and keeps it organized. It never decides what any of it means. You choose what goes in, and it stays yours.
>
> [I understand]

**Structure is part of the wording.** The three bold lines are headings, not emphasis inside a paragraph, and each is followed by its own short body. A person can read one block, look up at a nurse, and come back without losing their place. Built as a single wall of text it was skipped rather than read.

**One line is deliberately absent and is not to be restored.** The old third paragraph ended "and you are responsible for what you write down." Removed on the owner's decision, D32: the app already says it never decides what anything means, which is the honest part, and the rest was the software bracing against the person on the first screen they ever see.

The same substance appears in the About screen and the store listing. It is not shown again after acceptance. No version of the app skips it.

---

## 12. Accessibility floor

**Not a phase. A gate on every screen, and it stands unchanged from the previous direction.**

- 48dp minimum touch target everywhere, including rows that look smaller.
- Visible focus state on every focusable element, a 2dp `blue` outline offset 2dp.
- Complete screen reader labels on every control, including the FAB, every chart, every pill, and every gesture-revealed action. A chart's label reads the measure, the range of dates, the latest value, and the presence of any gap. A pill's label reads its state as a word.
- Contrast meeting AA in both themes at real sizes, measured and recorded, per 4.6.
- Dynamic type respected to the largest system setting without clipping, overlap, or lost actions. **Test at the maximum, not one step up.**
- Reduced motion respected, verified with the setting on.
- Every screen operable one-handed on a large phone, with primary actions in the lower half.
- No information carried by color alone, anywhere.
- Screen reader traversal order matches visual order, verified with the reader on.
- **Anything gesture-only also has a visible, non-gesture path.** A swipe nobody discovers is a feature nobody has.
- **Nothing important sits where a one-handed thumb cannot reach it**, which is a layout constraint rather than a nicety.
- **Date controls** are fully operable by reader and at maximum font size, and a reader reads an imprecise date as the person expressed it rather than as a resolved timestamp.

**Every touchable node says what it is, and the build checks it.** `ScreenReaderTest` walks a screen's semantics tree, in every window including a sheet's own, and fails on any node carrying a click that has neither text nor a content description. **It walks every one of them, and a check holds that to the directory rather than to anybody's memory**: `check_reader_coverage.py` fails on a screen `ScreenReaderTest` does not construct. **It said "every screen" once before and walked 44 of 75**, which is why the claim is now held by something rather than made. #342, and D133 is why the set lives in the directory.

That half is automated. **It does not replace running the reader by hand**, because traversal order and how a label actually sounds still need ears.

**A control that does nothing is removed, not labeled.** Naming it would put a control in a reader user's path that does nothing they can use, which is worse than an honest absence.

**Verified means with the setting on.** The reader running, the font at maximum, reduced motion actually enabled, on the phone. **Reading the code proves nothing here.** Per `CLAUDE.md` rule 19, the phone's accessibility settings may be changed for this verification provided the prior value is recorded before changing it and restored exactly afterward.

---

## 13. How to design a screen the grid does not show

**This is the method, and it is what makes onboarding, settings, edit sheets, and error states feel like the same app.** Work through it in order.

1. **Name the one thing.** The single reason the person opens this screen, in one sentence. That becomes the hero at display scale. **If you cannot name one thing, the screen is two screens.**
2. **Name the person's state.** Standing in a hallway with seconds, or sitting with ten minutes. **Design the calm state for the hallway**, and put the depth behind folds for the person with time.
3. **Cast the costumes.** List every element and assign each one exactly one costume or bare. **If an element resists classification, it is doing two jobs. Split it.**
4. **Plan year three.** What does this screen hold after three years of daily use, and which of law 4's four tools does it need. Then design its empty, one, few, and many shapes.
5. **Ask what the situation changes.** Whether the template should alter this screen's defaults, ordering, or presets.
6. **Place it in the binder.** Which tab hue and wash it inherits, or whether it is a whole-app surface using gold and the base ladder.

Then **compare it against the two nearest screens in the grid and make it read as their sibling.** Add it to the map in section 14, naming which drawn screen it follows, so the mapping stays complete.

### 13.1 Do not stop, and do not ask

Build the screen. Then log it. **Both parts are required and neither substitutes for the other.**

### 13.2 Compose, do not design

An undrawn screen is **assembled from section 7**. It is not an opportunity to introduce a new component, a new layout idiom, or a new way of presenting information the app already presents somewhere else.

**The test:** if you find yourself designing, you have already gone wrong. You should be composing.

**When a genuinely new pattern is unavoidable**, define it once in section 7 with its states and its rules, then use that definition everywhere it applies. **A pattern that appears twice in two different forms is a defect**, and the fix is to correct the earlier one rather than leave both standing.

### 13.3 Complete means complete

Every screen ships complete whether or not it was mocked up. **All of these, not most of them:** the empty state, the one-item state, the many-item state, the partially-filled state, the long-text state, the longest-language state, the loading state, the error state, and right to left.

**A screen without its empty state is not built.** The person must never encounter a blank area, a placeholder string, a stub, a debug label, a truncation, or a layout that only holds together because the sample data happened to be tidy.

**Overscroll, keyboard-up, and largest-font-size states are designed, not endured.**

### 13.4 Log it in three places, immediately

At the moment the screen is built, **not at a phase gate.** A screen built on Tuesday and logged on Friday is three days of work built on top of an unreviewed decision.

1. **An issue labeled `needs-design-review`**, with a real device screenshot, saying which screen, why it was needed, what it was composed from, what you deliberately did not invent, and what you were unsure about.
2. **An entry in section 14 of this document.**
3. **A line in the running list in `HANDOFF.md`.**

### 13.5 Discoverability is part of the screen

Every section, template, and feature must be reachable and, more importantly, **discoverable by someone who does not already know it exists.** A capability findable only by a person who already knew to look for it is not finished.

---

## 14. The undrawn-screen map

**Rebuilt clean on 2026-08-03 from the twenty-five screens that actually exist**, correcting the stale and self-repeating list in the grid's Part C, per section 3 correction 2. **Chapters and Appointments are drawn, at 19 and 22, and are not in this list.**

Every screen the app needs that the grid does not draw, and the drawn screen it follows:

| Undrawn screen | Follows | Why that one |
|---|---|---|
| Starting a project, what the template sets up | 04 Starting one | The grid draws it as a screen; law 3 makes it the second stage of a sheet. **Built 2026-08-05**, #277, review at #309 |
| The starting steps, changed | 18 The project's setup | Screen 18 draws the row and not what is behind it. **Built 2026-08-05**, #291, review at #310 |
| The road, changed | 18 The project's setup | The same, and it draws the road it edits. **Built 2026-08-05**, #291, review at #311 |
| The date kinds, changed | 18 The project's setup | The same, minus reordering: chips have no order. **Built 2026-08-05**, #291, review at #312 |
| The usual papers, changed | 18 The project's setup | The same. Not screen 13, which holds the paper itself and is unbuilt. **Built 2026-08-05**, #291, review at #313 |
| Keeping a project as a template, and who it is waiting on | 18 The project's setup | Two blocks on a drawn screen that the drawing does not carry. Both came off the superseded detail screen and the grid does not say where they go. **Built 2026-08-06**, #314, review at #317, decided in D118 |
| Standing instructions, the list | 12 Medications | A record of what was asked, folded by state, with `stone`. **Built 2026-08-04**, #185, review at #225 |
| One standing instruction | 10 One incident | What was asked, then every violation on a spine |
| One person | 09 One entry | Identity, then their own facts, then everything connected |
| One medication, and how it changed | 10 One incident | A history in order is a spine |
| One chapter | 19 Chapters | The current place opened, holding what it holds |
| One care thread | 10 One incident | A thread is a sequence with an outcome |
| Care threads, the list | 12 Medications | Peers with state, folded when ended. **Built 2026-08-04**, #186, review at #223 |
| One document | 09 One entry | The paper itself, then where the original lives |
| One bill | 09 One entry | The amount, its state, and what it is linked to |
| One appointment | 09 One entry | The facts, then the prep, then the calendar hand-off. **Built 2026-08-04**, #197, review at #232 |
| The prep sheet | 15 One project | The same screen. One appointment **is** its prep sheet, and building two would have been two answers to one question. The spine went to the changes, which are a sequence; the questions took screen 21's grouping, which is the shape they actually have |
| One question set | 21 Fifty questions | The pinned group, opened |
| One test round | 20 Tests and results | The round card, opened |
| One test's history | 24 Tracking one thing | A chart of one thing over time |
| Month review | 08 The trail | A period of the trail, composed, every line tapping through to its source. **Built 2026-08-04**, #200. Reached from the trail's own month heading, which gained a chevron and opens the month it names: the door costs no furniture and sits where the eye already is. **It has no total, deliberately.** A gold band reading "Written down this month, 42" was built and removed the same day: at that weight it competed with the hero, which law 1 says means the screen is wrong, and a single number over a month is an invitation to compare it against last month's, which is a judgment about somebody's care. Each group counts itself instead. Review at #236 |
| The milestone arc | 19 Chapters | A spine of milestones with gold rings. **Built 2026-08-04**, #200, and it is where the app first became able to read or write a milestone at all: the table shipped in the schema and nothing touched it, #234. Reached from the chapters list, because there is no thirteenth section. The chapter door wears the chevron row rather than the outlined pill it was first built with: law 2 gives that costume to a verb and gives navigation the chevron, and a place name is a noun. Review at #235 |
| Projects, the list | 12 Medications | Peers with state, active first |
| Starting a project | 23 Track something new | A grouped, searchable picker. **Converted 2026-08-04**, #201. The sixteen were a flat wall of cards and are rows in folds. **The grouping needed a category the data did not have**, so the templates gained one, held to `paying`, `challenge`, `moving`, `papers` by `check_templates.py`: it is what the person is trying to do, never what kind of office it involves, and never `phase`, which is build order. The person's own lead when they have any, the first category otherwise. Review at #239 |
| The template library | 23 Track something new | Same picker shape, the person's own first. **Converted 2026-08-04**, #201. **It reports rather than offers**, so what has produced something leads as cards holding its projects, and the rest folds by the picker's four categories. Fifteen of sixteen say "nothing started from this yet" in a real notebook, and sixteen cards mostly reporting nothing is the uniform weight rule 15 names. Review at #240 |
| One template | 15 One project | Steps in order, editable in place |
| The long road project home | **Projects grid, screen 05** | Drawn, so it is here only for what the grid does not draw: what the folds under the three answers hold. **Built 2026-08-05**, #278. The three answers, then steps and papers folded and counted. **The people fold and the trail fold were left out rather than opened onto nothing**, since both need screens that do not exist yet. Two defects were found by looking at it: the countdown at `monoL` out-shouted the lead on a project whose whole shape is that it leads with where it stands, so `DateRow` gained `prominent` and only the closing window draws it large; and the step checkbox was a glyph in a fixed box, so it clipped at font scale 2.0 and read as damaged, and it is drawn now. Review at #304 |
| Export | 07 Filing | One thing, then the passphrase confirmed twice, one filled action. **No encryption choice, because there is no unencrypted export**, `contract/DATA-CONTRACT.md` 8.1 and D84 |
| The date picker, zoomed to months and to years | Any screen that asks for a date | The grid draws the picker with a month name between two arrows and a day grid, and draws nothing for zooming out. **Built 2026-08-10**, #132, decided in nothing because nothing was in dispute: the heading became the control. **`CalendarCell` is `DayCell`'s treatment on a wider cell**, same wash, same ring, same today dot, same press and focus helpers, because two calendars in one sheet that looked like two different controls is what section 11 exists to prevent. **Month names in full**, since three letters is a different word in every language and a worse word in most. **Sixteen years a page anchored to the newest year**, so the current decade never lands split across two. **The year view's heading is a label rather than a button**, because there is nothing above it and a button that did nothing would be rule 11. Review at #337 |
| The export finished and could not find a file | 07 Filing | The grid draws Export with one outcome and there are two: the file was written, and something the database points at is not in it. **Built 2026-08-10**, #332, decided in D129. **Under "Saved" rather than instead of it**, because the file exists and every other row is in it, so turning a finished export into a failure would leave somebody holding no archive at all. **"Saved" keeps the hero** and the block below is grouped under a quiet mono eyebrow with no display type of its own, so the exception reads as an exception. **The consequence gets its own paragraph in `ink2`**: an archive naming a file it does not carry will not open, so "keep any earlier copy" is the half that is actually useful. Nothing new was drawn: `GroupHeader`, two `Text`, and `alertInk`, which is the same treatment as the passphrase warning on the same screen. Review at #335 |
| Import and restore | 07 Filing | Show what the file holds before writing anything |
| Merge or replace, on the restore screen | 07 Filing | The grid draws Import and restore as "show what the file holds before writing anything", and it draws one outcome. There are two, and 8.3 requires the choice to be explicit and in plain words. **Built 2026-08-09**, #211. **A radio group rather than two cards**, because it is one question with two answers each needing a sentence, and `ChoiceRow` came out of `AppearanceScreen` to be that group rather than a second drawing of the same shape. **Neither answer is preselected**: a default is the app guessing which of two very different sentences somebody meant, and one of them loses work. **The warning follows the choice** and the button says which of the two it will do, so the last thing read before an irreversible tap is what is about to happen. Review at #333 |
| What the merge decided | 09 One entry | Nothing draws it, and 8.3 requires that every resolution go "to a conflict log the person can actually open and read". **Built 2026-08-09**, #211. **Not the stored JSON**, per rule 20: each resolution shows only the fields that actually differed, with what was kept and what the other one said, and the columns that change on every write are left out. **Which columns can be shown is decided by the field map** rather than by hope, because a catalog key built from a column name throws in debug and would have crashed the screen the first time a conflict landed on a derived column. A deletion is the exception and carries its own word. **The door on More appears only once there is something behind it**: a permanent row reading "nothing to look at" teaches somebody to ignore a row that will one day matter. Review at #334 |
| The disclaimer gate | 05 Capture stage 1 | One question, one action, nothing else reachable |
| Setup | 05 Capture stage 1 | Staged conversation, skip always visible |
| The situation picker | 23 Track something new | Grouped and searchable, one tap to choose. **Converted 2026-08-04**, #202. The setting each group leads with keeps its card and its burden line; the rest are dense rows on paper. **No surface around the run**, per section 7's own rule about a list long enough to scroll, and **one lazy item per setting**, because the picker's test reaches every one of the fourteen by its own key and batching a group took those keys away. **No search and no folds**: fourteen rows is under two screenfuls, and this is the first screen after the disclaimer, where hiding an option behind a tap is the opposite of what it is for. Review at #242 |
| Change of situation | 19 Chapters | A chapter boundary, stated plainly. **Built 2026-08-04**, #202, and it had no door at all: the picker ran once during setup and was then unreachable, against law 5's promise that all of it is changeable afterward from one screen. It is a destination in More now. **The boundary is made rather than only stated**: `moveToChapter` ends the open chapter today and starts the new one today, because starting a second without ending the first left two places somebody was in at once. The chapter is offered and never made for them. Review at #241 |
| More | 03 Notebook | Destinations, so tiles and folds |
| Appearance and settings | 03 Notebook | Destinations and toggles, grouped |
| About | 09 One entry | Bare information, one door out |
| The family update draft | 15 One project | Composed lines, each a door to its source |
| The emergency card edit | 17 Emergency card | The same blocks, each with its Change |
| The date picker sheet | 06 Capture the note | A sheet over where you stand |
| Every add and edit sheet | 06 Capture the note | Sheets rise, one question at a time |
| Every error and empty state | 07 Filing | One thing, said plainly, with the one action |
| The notebook cannot be opened | 07 Filing | The Keystore key is gone, so there is no notebook behind this and no navigation out of it. **Three sentences and no action**: what happened, what it means for what the person wrote, and the file they exported. **It shipped for a month saying "That did not work. Nothing was changed."**, the copy for an action that failed, because `RootState` was private and nothing could compose it. Internal now, with `RootStatesTest`. **Offering restore from here is #343**, and until that lands the screen tells somebody to install the app again, which is honest and is not finished |
| The notebook is opening | 07 Filing | One quiet word on warm paper, the `Waiting` component. **No spinner and no progress bar**: neither knows how long a database read takes. Its KDoc described a spinner that was never in the code, which is how a component gets built to match a comment |

**Nothing in this app needs a new pattern.** If a screen seems to, re-read section 13 step 3: it is almost always doing two jobs.

| A question that has not been asked yet, opened | 21 Ask next time | The grid draws the list and no sheet for a question at all. A question still waiting had no tap, so the gesture was the only thing it answered to. **Built 2026-08-11**, #218, review at #338 |

**This table is kept complete.** A screen built from it gets its row confirmed. A screen discovered that is not on it gets a row added at the moment it is built, per 13.4.

---

## 15. Polish, added as you go

The small touches that separate finished from correct. **They apply everywhere, including to screens already built.**

- **Every touchable element has a visible press state**, on the standard spring, 120ms. **One treatment for the whole app.** A control that gives nothing back reads as broken.
- **List-to-detail navigation uses a container transform**, so the tapped card becomes the screen. **Sheets rise, never appear.**
- **New entries animate into place**, so the person sees where a thing went.
- **Folds animate open** rather than snapping.
- **All numerals everywhere are tabular**, so columns align. **Amounts right-align.**
- **Dates never fabricate precision the person did not give.**
- **Empty states** get a single line-art trail motif in ink at low opacity, drawn as a set at one stroke weight, plus two lines of warm copy: what this place is for, and the one action to start.
- **Icons are one coherent set at one stroke weight**, drawn for this app's sections and capture types, **never assembled from a default library.**
- **The trail mirrors correctly right to left**, including waypoints, scrubbers, and the FAB corner.
- **A paragraph that is the same for every row is said once for the screen, not once per row.** Standing instructions carried its tag explainer and its counting disclaimer under every instruction: three of them meant the same two hundred words three times, and what each instruction actually said was buried between them. Say it once, and where a paragraph belongs to a category rather than to the screen, say it on the first row of each category rather than only on the first row overall.
- **A mark is sized to itself, not to the system font.** An avatar's circle is a fixed 32, 40 or 56dp because it is a mark rather than words, so its initials are pinned at font scale 1 and every real word around them still scales. At 2.0 two initials stopped fitting and "AR" rendered as "A", which makes two people with the same first initial the same mark. That is the one place a fixed size is right, and it is stated so nobody generalizes from it.
- **The person's own words are isolated wherever they are rendered**, not only when joined. A dose of "500 mg, twice a day" in an Arabic layout renders as "mg, twice a day 500", and an allergy ending in a period gets the period moved to the front. Any `Text` carrying something a person typed, inside a layout that has a direction, goes through `Bidi.isolate`.
- **Any line built by joining parts is isolated, never concatenated.** A value, a unit and a date in one string are separate bidirectional runs, and in Arabic the algorithm reorders them against each other: `1.4 0 to 10 · 26 يونيو 2026` rendered as `2026 يونيو to 10 · 26 0 1.4`. `Bidi.join` wraps each part so it lays out on its own and the parts keep the order the code put them in. It applies to every joined line in the app, not only the one it was found on.
- **A list that has earned law 4's tools gives up a margin for them.** A rail drawn over the content looks right against short fixture text and cuts through the first real sentence that reaches the end of its line.

**When you notice a small refinement in this spirit that costs little, add it, and record it here**, so it becomes part of the system rather than a one-off.

### 15.1 Departures from the reference file, and why

Recorded here per section 19, rather than left for a later session to find and mistake for drift.

**Screen 08, the trail.** The grid draws a "Filter" control and a pin toggle in the header beside the search field.

**The filter is built and this entry said it was not**, corrected 2026-08-11 during the #345 fidelity pass. It filters **by kind**, as a row of outlined chips under the search field rather than as a control in the header, and it says how much it is hiding. **The question this entry filed rather than guessed at has been answered for one of its three parts**: kind. Filtering by thread and by unfiled are still not built, and whether a filter survives leaving the screen is still open.

**The pin toggle is still not built and the reasoning stands**: it would be a second way to reach a set the pinned group already shows at the top of the same screen.

**Screens 04 to 06, capture.** The grid draws the three stages as who or what, then when, then the note. This screen keeps the note first, which is the order it arrived at on the phone: somebody taps capture having just put a phone down and the thing in their head is what was said. Rule 15 puts the thing that matters most in the best position, and law 3's own goal, finishing in under fifteen seconds standing up, is met by typing one sentence and saving without ever seeing stages two or three.

**Screen 17, the emergency card.** The grid draws a Change pill on every block. That is four identical controls opening one editor, which is the same noise the trail's per-row pin turned out to be. There is one Change and one Share, at the top, where a person looks for the doors.

**Screen 13, Progress.** The grid draws the measures under the hero as chevron rows. A chevron means a screen opens, per law 2, and the per-measure history screen is #199 and does not exist yet. A chevron pointing at nothing is the dead end rule 18 forbids, so until that screen exists the rows are a choice: tapping one makes it the hero, which is the adaptive layout working rather than a substitute for it.

**Every screen with an in content action, its width.** The grid draws several of them full width. `SectionScaffold` uses that treatment at the foot of every screen to mean the way back, so an in content action wearing it competes with the one control that leaves. They are pills sized to their labels, D118 and #340. **A sheet is not affected**, because a sheet has no way back at its foot for anything to collide with, and **a filled action is not affected**, because it is a different costume: `PrepScreen`'s share and the restore screen's "Choose a file" are both full width and both filled, and both are the point of their screen.

**Every detail screen and two sheets, the removal control.** The grid draws no removal anywhere, because removal was a long press when the grid was drawn. It is an outlined "Remove this" at the foot of the thing's own screen now, and in the sheet its row opens where the thing has no screen. D135, and section 9 carries the rule.

**The person's screen, which the grid draws with two actions and now has three.** Calling them, correcting their details, and **writing something down about them**. The grid has no third control here and the screen had no way to record anything at all, so somebody who had just come off the phone with the charge nurse left, pressed the gold button, chose a kind, and found her name again in a picker. **Four taps to say a thing the app was standing next to**, which is exactly the shape rule 18 names. Added 2026-08-11, #46. **Recording sits above correcting**, per rule 15: it is the errand somebody actually came with, and correcting a spelling is the rare one. Both stay quiet pills sized to their labels, because the number above them is the one thing on this screen that carries weight.

**The eyebrow above a card, everywhere it appears.** A date beside a chapter name sat above an incident, a medication event, a search result and a project's spoken road, and three of the four joined the two with a literal separator while search used `Bidi.join` and the app's dot. **One treatment, per rule 22**, so all four now read the same and each part keeps its own direction inside a layout running the other way. Unified 2026-08-11. This is a departure only in the sense that nothing drew it: the grid never shows two eyebrows on one screen, which is exactly how three of them drifted apart.

**Screen 08, the pin itself.** The grid implies the pin is something you do from the trail. It is done from the entry instead. Built on the row first, it put a second target on every one of sixteen hundred rows to serve a decision somebody makes a handful of times, and on the phone ten pin buttons down one screen were the loudest thing on it, which inverts rule 15. The row keeps the mark as state.

---

## 16. Verifying against this direction

**Three checks, all run on the phone, on a real build, at both themes and in all shipping locales including Arabic. A screen is not converted until it has been seen on the device.**

### 16.1 The costume audit

On every screen:

1. Every interactive element wears **exactly one** costume from law 2.
2. **Nothing bare responds to touch.**
3. **Nothing costumed lacks a handler.**

**Any of those three fails the screen.**

### 16.2 The overflow audit

Every screen verified **at default and at maximum font scale**, with:

1. **No clipped content.**
2. **No element under the FAB.**
3. **The last scrollable item able to clear it.**

### 16.3 The judgment check

**Install it, hold it in one hand, and answer in one second:** what do you look at first, what can you touch, and where is everything else. **If any answer takes longer, fix the worst thing and look again.**

### 16.4 The checklist a screen passes before its issue closes

Checked rather than remembered, in this order.

1. **Built, installed to the phone, opened, and looked at on the real device.** Not a preview, not the layout inspector. **With the keyboard up on any screen carrying a text field**, which is the state the person actually spends their time in.
2. **Composed from section 7**, introducing nothing new.
3. **The one thing is nameable and is the largest thing on the screen.**
4. **The costume audit passes**, 16.1.
5. **The overflow audit passes**, 16.2, at both font scales.
6. **It holds up with real content**, not tidy sample data. Long names, empty sections, one item, many items, the year five fixture loaded.
7. **It holds up in the longest language and right to left.**
8. **Every state in 13.3 exists**, including the empty one.
9. **Accessibility, section 12, verified with the settings actually on.**
10. **Both themes.**
11. **It matches its grid**, 16.6. Side by side against the grid's drawing of that screen: spacing, radius, elevation, type scale, and composition.
12. **A device screenshot is committed**, checked first for real names and real contact details, and the screen is logged per 13.4.

**A screenshot that looks fine is not proof of any of this**, because it does not tell you what the screen does at another font size, in the other direction, with the year five fixture loaded, or with the keyboard up.

### 16.5 Retroactive, per `CLAUDE.md` rule 14

**Everything in this document applies to every screen already built, not only to new ones.** A codebase where the standard changed halfway through is a codebase with two standards. Every screen still on the old direction after the token pass is tracked as an open issue, so at any moment it is visible exactly what remains unconverted.

**Step 1 covers both themes, and no screen conversion begins before it completes.** D87. Every token, light and dark, including all six tab hues with their washes and ink variants, exists and is verified on the device at both themes first. **Converting screens against a light-only token set gives every converted screen a deferred second review**, which is the half-converted state step 1 exists to prevent. It does not save the work, it multiplies it by the number of screens.

---

### 16.6 The fidelity check, against the grid rather than against the prose

**Added 2026-08-11, D142.** A screen can pass every audit in this section and still not look like the drawing it was approved from. Three of the four checks above ask whether a screen is internally coherent; **none of them asks whether it matches what was approved.**

**How to run it.** Open the grid file's drawing of that screen beside a device screenshot of it, at the same width, and compare in this order:

1. **Spacing.** The gaps between blocks and the screen's own margins, against section 6 and the drawing.
2. **Type scale.** Which step each line is on. **The jump from 21sp to 13sp is meant to be felt at arm's length**, per 5.1, and closing it to make a screen look balanced is the most common way to lose the hierarchy.
3. **Radius and elevation.** Cards, pills, sheets, and whether anything is carrying a shadow the drawing does not give it.
4. **Composition.** What sits where, what is grouped with what, and what the eye lands on first.

**Where they differ, the grid is right unless a departure is recorded in 15.1 with its reason.** That is the existing rule and this check is what makes it enforceable rather than aspirational.

**The mechanical cause, so this is fixed rather than repeated.** Direction lives in prose here and measurement lives in the grids, so a screen built by reading this document alone never sees the numbers. Compounding it, **Compose Material 3 supplies its own defaults wherever a token is not explicitly applied**, so a component that looks finished may be wearing Material's shape, elevation, or typography rather than this app's. A theme audit for inherited defaults is part of this work.

**This does not redesign anything.** Everything it asks for was approved already. It asks the built screens to match it.

## 17. Banned, because they are current AI-design tells

Re-research current tells before any new screen work and add to this list. As of the last research pass, none of the following may appear anywhere:

Purple or indigo anything. Gradient fills, gradient text, gradient heroes. Glassmorphism, frosted panels, backdrop blur. Colored accent bars or colored left borders on cards. Cards outlined with a 1px gray border as their only definition. Three feature cards in a row. Numbered 01 / 02 / 03 markers where the content is not genuinely a sequence. Sparkle, wand, or magic iconography. A bounce on every hover or press. Inter as a display face. Emoji used as interface iconography.

Cards nested inside cards. The same label repeated in more than one slot of a single card. A large centered icon above a heading standing in for actual content. **Stat cards with small colored arrows beside numbers**, banned twice over here since this app never interprets a value. Status pills scattered everywhere as a substitute for real hierarchy. Press states that do nothing, and identical fade-in timing applied to everything. 3D blobs, plastic illustrations, and stock imagery of any kind. Everything visible at once with no progressive disclosure. Edge states left as afterthoughts, since empty, error, loading, offline, and partial are where generated interfaces are most obviously generated. Placeholder error copy, because "Something went wrong, please try again" removes the human voice at the exact moment the person needs it. Generic product phrasing: streamline, empower, supercharge, seamless, world-class, effortless. The manufactured-contrast cadence, "Not a form. A conversation."

**Slop is rarely ugly.** It is competent and anonymous, every screen out of one mold in a different coat of paint. **Anonymous is the failure mode to watch for here, not ugliness.**

**And understand the other failure mode as clearly as the first.** Avoiding generated-looking design does not mean avoiding design. **Plain is not a virtue here. Crude is not authenticity.** The target is a considered, warm, confident interface with real visual variety, not a stripped one.

---

## 18. What survived the v4 adoption, and what did not

Recorded so that no later session mistakes a deliberate deletion for an omission.

**Stood unchanged, because v4 does not touch this ground:** the dark theme derivation, 4.5. The accessibility floors, 12. The reduced motion rules, 10. The voice rules and the disclaimer wording, 11. The script coverage and locale rules, 5.3. The right to left rules, 5.4. The ban list, 17.

**Replaced outright, and the old paragraphs deleted rather than kept as history:** the previous light token set. The previous type scale and its roles. The previous spacing and radius scale. The previous component sections and the previous component library, both superseded by the single inventory in section 7. The previous screens section. The previous method for undrawn screens. The list of overrides against the old mockups, which described a reference file that no longer exists.

**Carried forward with correction:** the contrast method, which is unchanged, applied to the new palette and re-measured in 4.6. The 13sp floor and its two exemptions. The FAB, corrected in 8.

---

## 19. Keeping this document true

With every commit, ask whether the change made anything here wrong, and fix it in the same commit.

Any token added or changed during implementation is written back into section 4 or 5 **with its measured contrast ratio.** Any new component is specified in section 7 before it is built twice, **and it is not specified until it says when to use it and when not to.** Any screen that departs from `reference/screen-grid.html` gets its departure and reason recorded here, or gets corrected to match. Any new user-facing string follows section 11, and the ban list in section 17 gets re-checked against current research before any significant new design work.

**A design document that no longer matches the software is worse than no design document**, because the next session will build against it and inherit the drift.

---

## 20. Projects

**Governed by `reference/projects-grid.html`, adopted 2026-08-04**, D106. This section encodes that grid in full. Everything in sections 1 through 19 still applies: Projects is a whole-app surface, so it uses **gold and the base ladder** rather than a tab hue, per 4.3.

### 20.1 What a project is, and the three answers

**A project is a long process being carried for someone the person loves**: a waiver application, an insurance appeal, bringing a parent home, keeping a child's treatment covered. The person carrying it is tired and the process is slow and scattered.

**Every project screen answers one of exactly three questions, and the screen's shape is which one it leads with.**

1. **Where it stands.** Whose hands it is in, and since when. "The county is reviewing it. 23 days so far." **The elapsed time is a fact drawn from entries, never a judgment.**
2. **The next date.** A recorded fact carrying its source: "Apr 12, from the letter of Mar 5." A countdown in tabular mono. **A passed date reads "passed 6 days ago", plainly.**
3. **The latest word.** The most recent thing said by the office, the insurer, or the facility: who said it, when, and the reference number. It is what the person repeats at the start of every call.

### 20.2 Why the checklist could not carry this

**In a long process, most of what happens is not a task the person can check off.** They submitted the application; the county has been reviewing it for weeks. **A checklist shows that person a wall of unchecked boxes and makes the waiting feel like their failure.**

What carries families through is a complete record: every call with a name, a date, and a reference number, and no missed window. **The checklist remains, as steps, and leads only in the one shape where the work truly is many small arrangements.**

### 20.3 The three shapes, one grammar

| Shape | Leads with | For |
|---|---|---|
| **The long road** | Where it stands | Benefits and waiver applications, guardianship, a placement waitlist. Months between events. The trail folds by stage and the quiet stretches are drawn honestly: "23 days pass" |
| **The closing window** | The next date | Appeals, disputes, anything with a filing deadline. Under the countdown, what the file still needs before that date, each item there or **not yet** |
| **The busy stretch** | The steps | Discharge planning, a move, getting the house ready. Two intense weeks of small parallel arrangements. Steps clustered by area, each area headed with how many steps are in it and each step able to carry who is handling it. The one date everything converges on sits above |

**The shape is a default, never a cage.** It is one control on the project's setup screen and changes with no penalty.

### 20.4 How templates shape a project, exactly

**A project template is a bundle of five defaults, nothing more and nothing less**, and every one is visible, editable, and removable after setup:

1. **The stages.** The named stretches of the road, drawn as the road strip. Benefits: Applied, In review, Decision. Appeal: Decision received, Preparing, Submitted, Answered. Discharge: Date set, Arranging, Home. **Stages can be renamed, added, or removed; the road redraws.**
2. **The lead.** Which of the three answers opens the screen.
3. **The starting steps.** A short editable list of what people in this situation usually gather or arrange. **Suggestions of structure, not instructions to act**: every one can be deleted, and the app never says do this now.
4. **The usual papers.** Named placeholders, empty until filled. **An empty placeholder reads "not yet", never as an error.**
5. **The date kinds.** The kinds of dates this situation tends to have, offered as chips when a date is recorded.

**Applied once, at setup, as a preview the person confirms.** After that **there is no live link**: editing a project never touches the template, and updating a template never touches existing projects. The built-in bundles are Benefits or waiver, Insurance appeal, Discharge or move, and Blank, where Blank is one stage and the lead set to where it stands.

### 20.5 The eighteen screens

1. **Projects, before the first one.** The empty state says what this place is for and offers the one action.
2. **Projects, underway.** Project cards, each carrying its mini road and answering where it stands and the next date at a glance. On hold and closed fold away. **The FAB is the screen's filled action, so Start is outlined.**
3. **Starting one, 1 of 3.** One question per stage. The kind is chips.
4. **Starting one, 3 of 3, the setup shown.** The template's five defaults previewed before anything exists. **Nothing is applied until Create.** This screen is the template system made visible.
5. **The long road**, led by where it stands.
6. **The closing window**, led by the next date, with what the file still needs beneath it.
7. **The busy stretch**, led by the steps, clustered by area with a count of what is in each, and handler tags.

   The grid draws each cluster's count as `1 OF 3`, one done of three. **This draws how many steps are in the area instead**, because rule 13 rules out a completion count on the person's own work and the projects list already lost the same phrasing for the same reason. The difference is recorded in `DECISIONS.md` D116 and is the owner's to settle either way.
8. **Updating where it stands.** A one-stage sheet. Whose hands is chips drawn from the project's people; the date defaults to today. **Logging a call offers this automatically when things changed hands.**
9. **Logging a call, from inside the project.** Pre-answered with the project as a chosen chip. **The name and reference number are first-class**, because they are what makes the record usable later.
10. **One call, the whole record.** Every connection as a door, one outlined Edit.
11. **The trail of the project.** The spine scoped to one project. Stage changes land as gold-ringed waypoints. Quiet stretches drawn and dated.
12. **The road turns.** A sheet: the outcome as chips in the letter's own factual words, the letter photographed in the same motion. **The waypoint landing on the road is one of the three reserved expressive-spring moments**, per section 10.
13. **Papers of the project.** Camera first, with the one distinction that matters: **what they sent and what you sent**, each dated.
14. **People of the project.** Contacts with dialable numbers, separate from the care team. **The "also in" row is the cross-project door.**
15. **Everything, together.** The assembled collection as one PDF, generated locally and handed to the share sheet.
16. **Coming back after months away.** The project says what it held and offers one gentle way back in. **No shame, no streaks, nothing owed.**
17. **Closed, and kept.** The outcome in one line, the story in honest numbers, the record kept whole. **Leaf is allowed here: resolved is its meaning.** Reopen exists because these processes come back.
18. **The project's setup.** Law 5 made concrete: everything the template decided, changeable without penalty.

### 20.6 The five components this surface adds

Approved as inventory additions on 2026-08-04, D107. **Each is composed from existing costumes; no new colors and no new interactive grammar.**

| Component | What it is | Costume |
|---|---|---|
| **Road strip** | The project's stages as a horizontal stretch of the trail, waypoints and dashed rail. **Full** on the project home, **mini** on its card | **Bare.** It is information and does nothing on touch. Stages are edited from setup. Mirrors fully in RTL |
| **Standing card** | Where it stands: an eyebrow, a plain display sentence, the elapsed fact, and optional outlined actions | A grouped surface. Its actions are outlined pills, verbs or dialable numbers. **The sentence itself is bare** |
| **Date row** | A tabular countdown beside its fact and its source | **A row ending in a chevron**, opening the date's detail. The number is bare, mono, tabular |
| **Latest word card** | A gold wash band quoting the most recent entry, with attribution and reference line | A row with a chevron, opening the entry it quotes. Gold because the latest word is a trail entry surfaced |
| **Step row** | A checklist line with an optional handler tag | The box is the interactive element; **the handler tag is data, mono, bare** |

**The reference line, mono in a sand pill, is the standard dress for reference numbers everywhere in the app**, not only here.

**A handler tag is a label, not an account.** No notification, no assignment, no second user. The single point person model is untouched. D108.

### 20.7 What this surface never does

**It never advises**: it does not say file, call, escalate, or hurry. **It never casts anyone as an adversary or the process as a fight.** It records dates the person took from real papers, each showing its source, and states them as numbers, never as alarms. **It never colors by urgency**; the semantic colors keep their locked meanings. It never scores, streaks, or percents anything. It never treats a lapse as failure. Deliberately absent and to stay absent: progress percentages, urgency colors, streaks or scores, advice about what to do next.

### 20.8 To design a project screen this grid does not show

**Start from the three answers in 20.1 and ask which one this screen serves.** Then apply the undrawn-screen method in section 13 in full.

**Any new project shape must be a reordering of the existing components, never a new grammar**, and its template must be expressible as the five defaults in 20.4: stages, lead, starting steps, usual papers, date kinds. If it cannot be, it is not a project shape and the design is wrong somewhere earlier.

---

## 21. Today

**Governed by `reference/today-grid.html`, adopted 2026-08-04**, D106. Today is a whole-app surface and wears **gold and the base ladder**, and each card wears its own section's hue.

### 21.1 Law 1 versus modularity, resolved

**A free-form dashboard breaks the first law**: if the person can stack six equal cards, no screen has one thing first.

**The resolution is a fixed structure with free contents.** Today always has **exactly one lead slot** at the top, at display scale, **singular by construction**. Below it sits the card field, then the folds. **What fills the lead is the person's choice**: by default the digest, and any card can be promoted to lead from its options, which demotes the previous lead back into the field. **There is never zero and never two.**

**Customization decides what deserves the top; the law decides that something singular is at the top.**

**The universal search bar and the gold capture button keep their places regardless of layout**, because finding and recording are the two acts that must never move.

### 21.2 What a card is

**A card is a named, deterministic question asked of the record every time Today opens**: what is the next dated thing, how many are open, what was the latest value, who was seen last. Nothing more. **The answer renders; the card is a door to where the answer lives.**

Every card declares **one query** over the same single database. Queries run when Today gains focus and after any save. **No card ever computes an interpretation, a trend judgment, a recommendation, or a nudge.** The engine that writes the digest sentence is the same deterministic template engine used everywhere else.

**Anatomy, uniform across all cards:** a small index tab in the card's section hue and wash, naming its section in mono; the answer, sized by the card; a source or time line in quiet type; and **a corner chevron, because every card is a door**.

**Identity comes from the tab pack, never from decoration.** An appointments card is slate because appointments are slate everywhere in the binder. Cards for whole-app surfaces wear gold.

### 21.3 Three sizes, and how content adapts

| Size | Width | Carries | Touch targets |
|---|---|---|---|
| **Small** | Half | One answer, one line of context | **One.** The whole card |
| **Wide** | Full | The answer plus two or three lines, or one inline outlined action beside it | **Two** |
| **Tall** | Full, taller | A chart under the chart card's rules, a mini spine, or a short list. **Never a dense feed** | **Three** |

**Every card supports at least two sizes and declares its behavior at each.**

**Growing a card never adds a new kind of content, it reveals more of the same answer.** The medications card at small is a count; at wide it is the list. **Shrinking never hides the existence of something open, only its detail.**

**Inline actions appear only at wide and tall, always outlined, always a verb or a dialable number.**

### 21.4 The states ladder

**Every card defines its whole ladder before it ships.** This is where visual honesty lives and it is the difference between useful and decorative.

| Rung | What it is |
|---|---|
| **Full** | The normal render. Real values, real dates, tabular numerals |
| **Few** | Sparse-data grace. Two entries draws two dots and says "2 entries so far". **No line is invented between points far apart in time; gaps stay gaps** |
| **None yet** | **A calm state, never an error and never a scold.** The card names its one action. **Quiet is allowed to be good news** and the card says so: "Nothing waiting" |
| **Passed** | "passed 6 days ago", in plain words. **No urgency color, no alarm** |
| **Source closed** | A card pointing at a closed project says so and keeps working as a door. **Removed only by the person's hand** |
| **Returning** | The digest leads with "since you were last here" and plain counts. **No card ever measures the person's absence back at them** |

**Not every rung applies to every card, and chasing one that cannot exist is an evening wasted.** A card that answers with a number has no date to have gone by and no single source to close, so **passed and source closed belong only to the cards that point at something dated or at one row**: next up, a measure, milestones, the trail, and the three project cards. **Few means nothing on a card whose count is a yes or a no**, which is the emergency card. Everything else carries all six.

**Which fixture reaches which rung is a question with an answer**, and it is `python3 tools/checks/report_today_rungs.py`. It builds all six horizons and prints the table, including what no fixture produces, so "fixture data exists that produces each rung" is checked rather than assumed. **The empty Today is the one state no seed reaches**: it comes from clearing the app and walking onboarding.

### 21.5 Defaults beat blank canvases

**Nobody ever sees a blank Today.** The situation template chosen at onboarding ships a complete starting layout, which is law 5 doing the work.

**The catalog is curated, roughly sixteen cards, one per real recurring question, not one per table.**

**Two rules keep control where it belongs.** The app **never rearranges Today on its own, ever**. And every template default is a starting hand, editable from the first minute without penalty. **Personalization is the person's explicit choice plus their stated situation. It is never inference, because this app does not watch its user.**

### 21.6 The ten screens

1. **Today, as the template set it.** The digest leads. Search and capture fixed.
2. **Today, year three, made his own.** A chart promoted to lead, a project's date added, the digest demoted to a wide field card.
3. **The morning of an appointment.** Same layout as 2, different data. **Data moves; cards never move themselves.**
4. **Back after four months.** The digest takes the return voice regardless of which card holds the lead.
5. **Editing Today.** Entered by the **visible Edit button**; touch and hold is a shortcut, never the only path. **A card carries a remove dot and a drag handle and nothing else**, and the card itself opens its options. Inline, all of it was a wall: three chips and four named actions on every card is about a hundred and forty controls on a twenty card Today, at one weight, which is rule 15's uniform weight exactly. **Done saves; nothing saves behind your back.**
6. **Adding a card.** A sheet grouped by section in binder order, each entry previewing its small size **with real current data**. The situation's suggestions first, as a group of their own at the top, and a card is in one group only. **The whole row adds the card**, D123: rule 23 takes the easier target, and a full width row is one a thumb finds in a corridor. **The preview wraps**, per D105, because it is the thing being read to choose.
7. **One card's options.** Opened from the card in edit mode. Size chips, the source picker where the card takes one, Make this the lead, **Move up and Move down as the accessible reorder path**, and taking it off Today, which says in words that nothing written down is removed. **One sheet holds a card's whole life**, and law 3 is why it is one sheet: it asks one question, which is what this card should be. **Nothing here saves**; its Done closes the sheet and Today's Done is what writes.
8. **Largest font size.** The field reflows to one column at full width, nothing clipped, **layout order preserved exactly**.
9. **A different situation's default.** Two situations, two starting hands, one grammar.
10. **A quiet day.** **The dashboard's hardest state: it must not invent urgency to look useful.**

### 21.7 The card catalog

**Seventeen types. Anything not here does not exist as a card.**

| Card | Hue | Sizes | The question it answers |
|---|---|---|---|
| Today's digest | Gold | Small, Wide | What does the record say about today? Default lead in every template |
| Next up | Slate | Small, Wide | What is the next dated thing? |
| Medications | Teal | Small, Wide | What is on the list right now? **Record keeping only; never reminds, alarms, or tracks doses** |
| A measure | Moss | Small, Wide, Tall | What is the latest value and its recent shape? One card per chosen measure. **Chart card rules in full** |
| Milestones | Moss | Small | What was the most recent milestone? |
| Ask next time | Teal | Small, Wide | What is saved to ask, and for whom? |
| A project: where it stands | Gold | Wide | Whose hands, since when? |
| A project: the next date | Gold | Small | How many days to the date that matters? |
| A project: steps | Gold | Wide | What is in each cluster? Counts only |
| Incidents | Alert | Small | How many are open? **The one card where alert appears**, because that is alert's locked meaning |
| Money | Manila | Small, Wide | What is unresolved? Amounts only at wide, right-aligned |
| Unfiled | Gold | Small | Is anything waiting to be filed? |
| Emergency card | Red | Small, Wide | **None. This is pure access**, one tap to the hand-over screen |
| Care team | Rose | Small, Wide | How do I reach them, now? **Two variants and one question**: with a source it is one chosen person, their role, and **their number as an outlined pill at wide**; without one it is everybody, as a row of avatars with an overflow mark. The picker in the card's options is where the person chooses, 21.6 screen 7. **`ACTION_DIAL` and never `ACTION_CALL`**, per 9.1: the dialer opens filled in and nothing goes out until they press call. Somebody archived is the source-closed rung and is offered no number, because suggesting a call they have already decided against is the app having a view |
| The trail, lately | Gold | Tall | What were the last few entries? **A three-entry mini spine at tall, drawn with the same `SpineRow`, route and node colors the trail itself uses**, so somebody who learned the vocabulary there reads this without being taught twice. **The gap markers are the reason it is drawn rather than listed**, 5.2.4: two calls a week apart read as a week of calls and the same two rows four months apart read as somebody left alone, and a list shows them the same either way. Each row's eyebrow carries the date and the kind, because 2.2 says a color never carries meaning alone. **The waypoints are not doors**: the card is one, and 21.3 gives tall three targets, not four. At smaller sizes the newest entry is the answer and the rest are a list under it |
| Recent documents | Manila | Wide | What papers arrived lately? **Never renders private content larger than a thumbnail** |
| Standing instructions | Stone | Small | How many are active, and are any issues noted? |

### 21.8 What Today never does

**It never rearranges itself, promotes a card, or injects one, no matter what the data does.** The difference between screens 2 and 3 is data inside a layout the person owns, and **that distinction is the whole trust model of the surface**.

It never interprets: no trends called good or bad, no streaks, no scores, no advice, no urgency theater. **It never colors by value.** It never renders medical judgment, targets, or ranges on any chart at any size. **It never treats quiet as a failure to fix or absence as a debt to collect.** And it never loses the arrangement.

### 21.9 To design a card the catalog does not have

**In this order, and the first step can end it.**

1. **Name the recurring question it answers about the record.** **If you cannot name one real recurring question, the card does not exist.**
2. **Write its one deterministic query.**
3. **Define its full states ladder**, all six rungs of 21.4, **before any layout**.
4. **Design its sizes within the touch-target budget** in 21.3. Growing reveals more of the same answer, never a new kind of content.
5. **Cast its costume.** The card is one door; inline actions are outlined pills at wide and tall only.
6. **Place it in the binder.** Its section's hue and wash, or gold for a whole-app surface.

**A new card is a catalog change.** It **displaces or genuinely adds a question**, is recorded in 21.7 with all of the above, and **gets its states ladder into its issue's acceptance criteria**.

---

## 22. The voice rule these two grids make global

**Nothing anywhere in this app frames a person's situation as a battle, a game, or a race.**

**Banned in code identifiers and user-facing copy alike:** fight, battle, win, lose, opponent, the ball, having the last word, and any sports or war metaphor.

**People in a process are named by role, never cast as adversaries.** A caseworker is a caseworker.

**Urgency is stated as fact, a number and its source, never performed.** A date is a number and where it came from, not a warning.

**This applies to every surface, not only Projects**, and it joins `tools/checks/check_copy.py`. It arrived with the Projects grid because that is the surface where the temptation is strongest, and it was made global on adoption because the temptation is not confined there. D109.

---

## 23. Verifying the two surfaces

The costume audit, 16.1, and the overflow audit, 16.2, run on every screen in both grids as usual. **Two additions.**

### 23.1 The states ladder audit

**Every card type is verified on the device in every state of its ladder**, using fixture data that produces each state, **at both themes, at maximum font scale, and right to left**. **A card's issue does not close until every rung has been seen.**

### 23.2 The trust audit

**With fixture data changing underneath it, confirm Today's layout does not move**, and confirm **the lead slot is always exactly one thing**.

**The road strip, the edit mode drag, and the card field must mirror correctly right to left.** **Reorder must work by drag and by Move up and Move down**, so it works one-handed, with the reader on, and with switch access.

Everything on the phone, on a real build. No emulator.
