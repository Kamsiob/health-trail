# DESIGN.md, Health Trail by Kamsiob

This document is the binding source of truth for every visual, motion, and copy decision. Where code and this document disagree, this document wins. It is a living document: any decision made during implementation is written back into it in the same commit.

**The visual reference is `reference/screen-grid.html`.** Open it in a browser. It holds twenty-five drawn screens, numbered 01 through 25, plus the laws they were drawn from and the notes on how they hold together. This document gives the tokens, the real dp and sp values, the dark theme, the measured contrast, and the rules the mockups imply but cannot state.

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
| Display M | Bricolage | 19 to 20sp / 1.1 | 800, -0.02em | Screen titles, the current chapter, a round's reason |
| Row title | Bricolage | 13sp / 1.3 | 700 | The title line of a group row |
| Body | Atkinson | 13sp / 1.5 | 400 | Supporting content. **The floor** |
| Body S | Atkinson | 12sp / 1.4 | 400 | Folded rows on sand, row subtitles |
| Label | Atkinson | 13sp / 1.4 | 700 | Buttons and chips |
| Nav label | Atkinson | 11sp / 1.3 | 700 | Bottom navigation only. Exempt from the floor |
| Mono | JetBrains Mono | 11sp / 1.5 | 700, tracking 0.12em, uppercase | Eyebrows, counts, dates as data, amounts. Exempt from the floor |
| Mono L | JetBrains Mono | 22 to 30sp | 700, tracking 0, tabular | A number at display size, in the big-number component and nowhere else |

**The jump from 21sp to 13sp is large on purpose and is meant to be felt at arm's length.** It is what law 1 is made of. Closing it to make a screen look balanced is the most common way to lose the hierarchy.

**Mono L carries no tracking.** The 0.12em exists to raise letter distinction at 11sp. At 22sp and above the same tracking pulls the digits of one number apart until it reads as several numbers. Tabular figures do the alignment work instead.

**The two exemptions from the 13sp floor, and why they are the only two.** The nav label and the Mono metadata style sit below it. Neither ever carries information on its own: a nav label is always paired with an icon and a content description, and a Mono eyebrow or count is always directly beside the content it labels. Both are short, and Mono is uppercase and tracked, which raises its letter distinction well above 11sp lowercase body text. Both scale with dynamic type. **Nothing else may be added to this list.** A third candidate is a sign the layout is too dense, and the layout gets fixed rather than the floor lowered.

**The nav label is capped at 1.4, and it is the only capped type in the app.** At font scale 2.0 "Notebook" broke mid-word and collided with the capture button. Four labels and a fixed clearance for the FAB share one row, which is a width budget nothing else has. Above the cap the label holds and the icon, the position, and the content description carry it. Found on the phone with the system font at maximum, which is why section 12 requires that pass rather than a reading of the code.

### 5.2 The faces must be verified on the device before any screen work

**This is a gate, not a checkbox.** This app has already shipped multiple sessions of visual review that were invalid because a face had silently fallen back to the system typeface and nobody checked.

Before any screen work begins: screenshot a screen containing all three faces, confirm the letterforms by eye, and **record the confirmation in `HANDOFF.md`**. A review run against a fallback face is not a review.

### 5.3 Script coverage, which is a real problem rather than a detail

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
| **Wash band** | A section-colored summary strip, for a total or a state at a glance |
| **Spine with waypoints** | Filled done, ringed open, hollow upcoming, gold-ringed milestone. Dashed rail, and mono gap markers such as "six days pass" |
| **Avatar** | Initials in the section wash |
| **Stat and big-number** | Tabular mono |
| **Chart card** | Name, latest value, plain polyline with dots, **gaps drawn as gaps**. No target lines, no zones, nothing colored by value |
| **Round card** | For lab work. One card per draw, holding its tests, its reason, its documents, and who ordered it. **Every test name is a door to that test's own history** |
| **Agenda list and month grid** | For appointments, drawn only from what is recorded in the app |
| **Thumbnail gallery** | The person's own paper, visible |
| **Search bar** | Sand, rounded |
| **View toggle** | Remembered per section |
| **Pin marker and pinned group** | The person's own hierarchy, above everything |
| **Sticky section header** | So the person always knows where they are |
| **Edge scrubber** | Jump by year and month |
| **Sheet** | Rises from the bottom, 24dp top corners, scrim behind |
| **FAB** | Gold, 48dp, bottom-trailing corner, opening a labeled menu that blooms upward from it |
| **Bottom navigation** | Four tabs, even spacing, active tab marked with a tonal pill behind its icon |

**Any component in the current codebase that is not on this list is retired.** Do not keep a one-off around because a single screen uses it. Rebuild that screen from the inventory. The retirements are enumerated in `DECISIONS.md` D82.

**A component is not defined until it says when to use it and when not to.** That was the omission that let one card shape spread across twenty screens. Each entry above carries its "what it is for," and the choosing rule is law 2 read from the other direction: pick the costume from what the person is doing, and the component follows.

**Not everything is a card, and making everything a card is why nothing stands out.** A tile for a fixed set of destinations. A dense row for a long list somebody scans. A card only for something with three or more lines they actually read. One hero per screen at most. A spine for anything sequential. A thumbnail where the app holds the person's own paper. An avatar for a person.

**A screen also has a layout pattern**, and using the same one as the screen it was reached from is a deliberate choice rather than the default.

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

**Folds open in place** rather than on a new screen, unless what they hold exceeds a screenful.

**Every list ends in an add affordance in context**, prefilled with the context it was opened from, and the global gold capture exists everywhere besides it.

**Every detail screen has one outlined Edit.** A single field shows Change beside it.

**Tapping a row opens it.** Selection states exist only inside sheets. **Any swipe action also has a visible button path.**

**Every save button takes what is there.** Partial is a finished state. Empty reads as not yet and never as an error. **Drafts survive leaving the screen.**

**Everything named on a screen is a door, and the other side shows the way back.** If A shows B, then B shows A. A one-way link is a dead end wearing a disguise. Carry context forward instead of asking again, and count the taps: a flow that takes four when it could take two gets abandoned in a hallway.

### 9.1 Appointments stay offline by construction

The agenda and month views are **drawn only from appointments recorded in this app.** No account, no sync, and **no calendar read permission is ever requested.**

A single appointment offers to hand itself to the phone's own calendar app: one event, user-initiated, one way, **with nothing read back.**

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

**Every touchable node says what it is, and the build checks it.** `ScreenReaderTest` walks every screen's semantics tree, in every window including a sheet's own, and fails on any node carrying a click that has neither text nor a content description.

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
| Standing instructions, the list | 12 Medications | A record of what was asked, folded by state, with `stone` |
| One standing instruction | 10 One incident | What was asked, then every violation on a spine |
| One person | 09 One entry | Identity, then their own facts, then everything connected |
| One medication, and how it changed | 10 One incident | A history in order is a spine |
| One chapter | 19 Chapters | The current place opened, holding what it holds |
| One care thread | 10 One incident | A thread is a sequence with an outcome |
| Care threads, the list | 12 Medications | Peers with state, folded when ended |
| One document | 09 One entry | The paper itself, then where the original lives |
| One bill | 09 One entry | The amount, its state, and what it is linked to |
| One appointment | 09 One entry | The facts, then the prep, then the calendar hand-off |
| The prep sheet | 15 One project | A sequence the person walks in a room |
| One question set | 21 Fifty questions | The pinned group, opened |
| One test round | 20 Tests and results | The round card, opened |
| One test's history | 24 Tracking one thing | A chart of one thing over time |
| Month review | 08 The trail | A period of the trail, composed |
| The milestone arc | 19 Chapters | A spine of milestones with gold rings |
| Projects, the list | 12 Medications | Peers with state, active first |
| Starting a project | 23 Track something new | A grouped, searchable picker |
| The template library | 23 Track something new | Same picker shape, the person's own first |
| One template | 15 One project | Steps in order, editable in place |
| Export | 07 Filing | One thing, then the passphrase confirmed twice, one filled action. **No encryption choice, because there is no unencrypted export**, `contract/DATA-CONTRACT.md` 8.1 and D84 |
| Import and restore | 07 Filing | Show what the file holds before writing anything |
| The disclaimer gate | 05 Capture stage 1 | One question, one action, nothing else reachable |
| Setup | 05 Capture stage 1 | Staged conversation, skip always visible |
| The situation picker | 23 Track something new | Grouped and searchable, one tap to choose |
| Change of situation | 19 Chapters | A chapter boundary, stated plainly |
| More | 03 Notebook | Destinations, so tiles and folds |
| Appearance and settings | 03 Notebook | Destinations and toggles, grouped |
| About | 09 One entry | Bare information, one door out |
| The family update draft | 15 One project | Composed lines, each a door to its source |
| The emergency card edit | 17 Emergency card | The same blocks, each with its Change |
| The date picker sheet | 06 Capture the note | A sheet over where you stand |
| Every add and edit sheet | 06 Capture the note | Sheets rise, one question at a time |
| Every error and empty state | 07 Filing | One thing, said plainly, with the one action |

**Nothing in this app needs a new pattern.** If a screen seems to, re-read section 13 step 3: it is almost always doing two jobs.

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

**When you notice a small refinement in this spirit that costs little, add it, and record it here**, so it becomes part of the system rather than a one-off.

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
11. **A device screenshot is committed**, checked first for real names and real contact details, and the screen is logged per 13.4.

**A screenshot that looks fine is not proof of any of this**, because it does not tell you what the screen does at another font size, in the other direction, with the year five fixture loaded, or with the keyboard up.

### 16.5 Retroactive, per `CLAUDE.md` rule 14

**Everything in this document applies to every screen already built, not only to new ones.** A codebase where the standard changed halfway through is a codebase with two standards. Every screen still on the old direction after the token pass is tracked as an open issue, so at any moment it is visible exactly what remains unconverted.

**Step 1 covers both themes, and no screen conversion begins before it completes.** D87. Every token, light and dark, including all six tab hues with their washes and ink variants, exists and is verified on the device at both themes first. **Converting screens against a light-only token set gives every converted screen a deferred second review**, which is the half-converted state step 1 exists to prevent. It does not save the work, it multiplies it by the number of screens.

---

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
