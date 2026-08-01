# DESIGN.md, Health Trail by Kamsiob

This document is the binding source of truth for every visual, motion, and copy decision. Where code and this document disagree, this document wins. It is a living document: any decision made during implementation is written back into it in the same commit.

**The visual reference is `reference/screen-grid.html`.** Open it in a browser. It holds the sitemap card plus 27 approved screens and it is what the built app must look like. This document gives the tokens, the real dp and sp values, the dark theme, and the rules the mockups imply but cannot state. Where the mockups and this document differ, this document wins, and every such difference is listed in section 3 with its reason.

`reference/concept-review.pdf` is the same 27 screens as a reading document, useful for seeing the intended sequence and voice.

---

## 1. Direction

**A trail map, not a medical chart.**

The person opening this app is exhausted, often standing in a hallway, and holding a phone in one hand. Nothing on screen may feel clinical, gamified, or cheerful. The app should feel like a well made paper notebook that happens to be searchable: warm, quiet, legible, and completely unexcited about itself.

The metaphor is a marked trail. A journey through places, with a route you can follow backward. It shows up in three places and nowhere else: the two stacked gold bars of the mark, which are a painted trail blaze; the dashed gold line running behind the timeline; and the colored dashed routes that identify parallel care threads.

**The one bold thing is the trail.** Everything else stays disciplined. If a decoration does not carry information, it does not ship.

### Banned, because they are current AI-design tells

Re-research current tells before any new screen work, per the universal standards, and add to this list. As of the last research pass, none of the following may appear anywhere:

Purple or indigo anything. Gradient fills, gradient text, gradient heroes. Glassmorphism, frosted panels, backdrop blur. Colored accent bars or colored left borders on cards. Cards outlined with a 1px gray border as their only definition. Three feature cards in a row. Numbered 01 / 02 / 03 markers where the content is not genuinely a sequence. Sparkle, wand, or magic iconography. A bounce on every hover or press. Inter as a display face. Emoji used as interface iconography.

**Added 2026-07-31, from the research pass behind D33.** Cards nested inside cards inside cards, because depth is not hierarchy. The same label repeated in more than one slot of a single card. A large centered icon above a heading standing in for actual content. Stat cards with small colored arrows beside numbers, banned twice over here since this app never interprets a value. Status pills scattered everywhere as a substitute for real hierarchy. Press states that do nothing, and identical fade-in timing applied to everything. 3D blobs, plastic illustrations, and stock imagery of any kind. Everything visible at once with no progressive disclosure. Edge states left as afterthoughts, since empty, error, loading, offline, and partial are where generated interfaces are most obviously generated. Placeholder error copy, because "Something went wrong, please try again" removes the human voice at the exact moment the person needs it. Generic product phrasing: streamline, empower, supercharge, seamless, world-class, effortless. The manufactured-contrast cadence, "Not a form. A conversation."

**Slop is rarely ugly.** It is competent and anonymous, every screen out of one mold in a different coat of paint. Anonymous is the failure mode to watch for here, not ugliness.

The accent bar one is specifically called out because it already slipped into a draft of the care threads screen and had to be removed. Thread identity is carried by a dashed route line, which is the app's own metaphor, not by a colored edge, which is everyone's default.

---

## 2. Color

### 2.1 Light theme, the canonical values

These are exactly the values in the mockups.

| Token | Hex | Use |
|---|---|---|
| `paper` | `#FAF6EE` | App background. Warm, never white. |
| `card` | `#FFFFFF` | Card and sheet surfaces. |
| `sand` | `#F1EBDC` | Recessed surfaces: icon tiles, avatars, inset rows, disabled chips. |
| `ink` | `#22384A` | Primary text. |
| `ink2` | `#5A6D7C` | Secondary text. Corrected from `#5C6F7E`, see 2.3. |
| `ink3` | `#96A4AE` | **Non-text only.** See 2.3. |
| `blue` | `#2F6F8F` | The single accent. Actions, filled buttons, links, the PT thread. |
| `blue_deep` | `#245A75` | Pressed state, text on `blue_soft`. |
| `on_blue` | `#F3FAFD` | Text and icons on a `blue` fill. |
| `blue_soft` | `#E3EEF3` | Tonal chips and tinted icon backgrounds. |
| `blaze` | `#D99D2B` | **Shapes only.** The mark, the trail line, timeline nodes, the capture button. |
| `blaze_soft` | `#F7ECD1` | Gold tonal backgrounds: waiting-on cards, flag tiles. |
| `leaf` | `#4E8A5C` | **Shapes only.** Resolved and progress indicators. |
| `leaf_soft` | `#E4EFE5` | Green tonal chips. |
| `alert` | `#B84A2E` | Emergency card only, plus open-incident dots and the disputed state. |
| `alert_soft` | `#F8E4DB` | Red tonal chips, open-incident pills. |

Care thread route colors, light theme: physical therapy `#2F6F8F`, occupational therapy `#4E8A5C`, speech `#B36A3C`, nursing `#6E7F5A`. A fifth and sixth are generated by rotating hue within the same saturation and lightness band, never by picking a brighter color.

### 2.2 Color discipline, non negotiable

- **One accent: `blue`.** Every action, every button, every link.
- **`blaze` is reserved for the trail metaphor and the capture button.** It is not a second accent. It never fills a button that is not the capture button, never highlights a row, never colors text.
- **`alert` belongs to the Emergency Card.** Its only other uses are the open-incident dot, the open-incident pill, and the disputed-bill pill. It never appears as a warning about a measurement, ever, because the app does not judge measurements.
- **`leaf` means resolved or completed.** Nothing else.
- **Color is never the only carrier of meaning.** Every state that has a color also has a word, a shape, or an icon. An incident is not "the red one," it is the one whose pill says OPEN.
- **No pure black, no pure white background.** `card` is white as a surface on warm paper, which is different and intended.

### 2.3 Text contrast corrections, measured

The mockups use several colors as small text that do not meet WCAG AA at real text sizes. These are corrections, not suggestions, and they are the reason this section exists.

**Everything below is measured rather than calculated,** by `tools/checks/check_contrast.py`, which reads the tokens out of the theme itself and runs on every push. An earlier version of this section carried calculated numbers and said plainly that the measurement was what counted. It was right to: three of its four proposed corrections did not clear the floor once measured against the actual surfaces, because they had been calculated against white rather than against warm paper.

| Problem in the mockups | Correction | Measured, at its tightest surface |
|---|---|---|
| `ink3` `#96A4AE` used for eyebrows and timestamps, about 2.4:1 on paper. | `ink3` **text** uses `#5C6C77`. The original stays valid for non-text only. | 4.57:1 on `sand` |
| `ink2` `#5C6F7E` measured 4.38:1 on `sand`, just under the floor. | `#5A6D7C` | 4.51:1 on `sand` |
| `blaze` `#D99D2B` as text, about 2.2:1. | Gold text is `#8F6309`. `blaze` itself never renders text. | 4.52:1 inside a gold tonal card |
| `leaf` `#4E8A5C` as text, about 3.8:1. | Green text is `#3D7049`. `leaf` remains the shape color. | 4.92:1 inside a green tonal chip |
| `alert` `#B84A2E` measured 4.22:1 as text inside a red tonal pill. | Alert text is `#B34529`. `alert` remains the shape color. | 4.50:1 inside a red tonal pill |

**Floors.** 4.5:1 for text under 18sp. 3:1 for text at 18sp and above, and for user interface components and graphical objects required to understand content.

**What is decorative, and why that is not a loophole.** A hairline rule, the dashed trail line, a timeline node, and a care thread route are measured and reported but are not held to 3:1. WCAG 1.4.11 covers interface components and graphics required to understand content, and none of these are: remove a hairline and nothing becomes unreadable, and a node's color is never the only thing carrying its meaning, because section 2.2 requires a word, a shape, or an icon alongside it.

The alternative would be forcing the trail to stop being gold, and gold is the entire metaphor. So these are measured on every run, printed, and reviewed by eye on a device rather than ignored. For the record, in light theme the trail line sits at 2.21:1 on paper and a hairline at 2.37:1.

### 2.4 Dark theme

The dark theme is a trail map at dusk, not an inverted document. Surfaces get lighter as they come forward, and elevation is carried by surface lightness rather than by shadow. Gold and red must keep their exact meanings.

| Token | Hex | Notes |
|---|---|---|
| `paper` | `#121A20` | Deep blue slate. Never black, which smears on OLED during scroll and is harsh in a dark room, which is when this theme gets used. |
| `card` | `#1A242B` | One step forward. |
| `sand` | `#223038` | Recessed surfaces read as slightly lighter here, the opposite of light theme, which is correct for dark surfaces. |
| `ink` | `#E9EEF1` | Primary text. Never pure white. |
| `ink2` | `#A6B4BD` | Secondary. |
| `ink3` | `#8798A1` | Text safe in this theme, measured 4.55:1 on `sand`, which is the tightest pairing in the dark theme. `#7F9099` measured 4.10:1 and was not safe. Non-text may go to `#66757E`. |
| `blue` | `#7FB6D4` | Lightened so it carries text contrast on dark. |
| `blue_deep` | `#9BCBE4` | Pressed and emphasis. |
| `on_blue` | `#0B171E` | Dark text on the light blue fill. Filled buttons invert in dark mode. |
| `blue_soft` | `#1E323D` | Tonal. |
| `blaze` | `#E3B155` | Shapes: mark, trail, nodes, capture button. |
| `blaze_text` | `#E9BE6E` | Gold text. |
| `blaze_soft` | `#33290F` | Gold tonal backgrounds. |
| `leaf` | `#74B383` | Shapes. |
| `leaf_text` | `#8CC79A` | Green text. |
| `leaf_soft` | `#16291C` | Tonal. |
| `alert` | `#E58163` | Open and disputed states. |
| `alert_fill` | `#A8412A` | The Emergency Card header fill, with white text. |
| `alert_soft` | `#3B1E14` | Tonal. |

Thread routes, dark: PT `#7FB6D4`, OT `#74B383`, speech `#D0946A`, nursing `#9CAE85`.

The capture button stays gold in both themes. It is the one element whose color does not shift meaning between themes, because it is the single way data enters the app and it must be findable without thought.

**Its glyph is not white.** White on `blaze` measures 2.38:1 in light and 1.97:1 in dark, well under the 3:1 a control needs. The fill stays gold, which is what carries the meaning, and the glyph darkens: `#22384A` in light, measuring 5.08:1, and `#0B171E` in dark, measuring 9.25:1. This matters more than it would on most buttons, because this is the one control the app cannot afford to have anyone miss.

### 2.5 Elevation

Light theme, the mockup shadow, two layers: `0 8dp 24dp rgba(50,62,42,0.09)` plus `0 2dp 5dp rgba(50,62,42,0.05)`. Soft, warm, low contrast. Never a hard drop shadow.

Dark theme: no shadow. Elevation is `paper` to `card` to `sand`, plus an optional hairline of `rgba(255,255,255,0.06)` where two surfaces of the same value must separate.

Note for anyone generating print or PDF output from these styles: large soft shadows rasterize as dark smudges. Print and PDF paths substitute a 1dp hairline. This was learned the hard way producing the concept document.

---

## 3. Where this document overrides the mockups

Every difference, with its reason. Nothing else differs.

1. **Text contrast**, section 2.3. The mockups were drawn at a reduced scale where the light grays read acceptably. At real text sizes they fail AA.
2. **Dark theme**, section 2.4. Does not exist in the mockups.
3. **Real dp and sp values**, section 4. The mockups are drawn inside a 252px-wide phone frame representing roughly 393dp, so every pixel value in the HTML is about 0.64 of its real value. Do not multiply blindly; use the token scale in section 4, which was derived from the mockups and then rounded onto the 4dp grid.
4. **Minimum text size is 13sp,** with exactly two exemptions, listed in section 4.3 and nowhere else. Several mockup labels scale to 11sp or less. Those move up to 13sp, and the layout absorbs it. Nothing in this app is too dense to read.
5. **Touch targets are 48dp minimum** regardless of the visual size of the thing being tapped. Several mockup rows are visually shorter than that and get invisible padding to reach it.
6. **Care thread accent bars are removed**, replaced by dashed route lines. Already corrected in the reference file; stated here so it is never reintroduced.
7. **The capture overlay is a real bottom sheet**, not the dimmed-background composite shown in two mockup screens. The mockups show it that way only because a static image cannot show a transition.
8. **The table of contents carries twelve rows in four labeled groups, at three weights.** The reference draws eight rows, flat, at one weight, with a body-face descriptive subtitle under each. Four differences, each with its reason. Twelve rather than eight, because `MASTER_SPEC.md` section 4.4 fixes the twelve sections and the reference simply drew a subset. Group headers, because twelve at uniform weight reads as a list of everything rather than as a table of contents, which is the defect issue #36 exists to fix; the groups add structure around the existing order and move nothing. Three weights rather than one, because the situation templates carry `forward` and `folded` arrays that the reference predates, and D33 records how they render. One Mono count string per row rather than a descriptive subtitle, because "12 people" and "214 entries · 1 open incident" are two different treatments of the same slot and the counts have to be one treatment to be scannable; the descriptive form returns as each section's own screen lands and can say something true about itself.
9. **The mark's bars are thinner than the reference draws them.** The reference sets each bar at 22 by 9 with a 3 gap. Built at icon scale that weight reads as a hamburger menu control rather than as a painted blaze, which was confirmed by looking at it on a device. The bars are 22 by 6 with a 5 gap and capsule ends. Equal widths are unchanged and are not negotiable, see 5.1.

---

## 4. Layout, type, and shape

### 4.1 Spacing

4dp grid. Tokens: 4, 8, 12, 16, 20, 24, 32, 40. Screen horizontal padding 20dp. Card internal padding 16dp. Gap between cards 12dp. Gap between a section header and its content 12dp. Gap between sections 24dp.

### 4.2 Radii

Card 20dp. Inset tile, icon tile, chip container 12dp. Thumbnail 8dp. Bottom sheet top corners 28dp. Pill and button fully rounded. Avatar circular. Bottom nav container 24dp.

### 4.3 Type

| Role | Face | Size / line | Weight | Use |
|---|---|---|---|---|
| Display L | Bricolage Grotesque | 28sp / 34 | 700, tracking -0.02em | Screen titles |
| Display M | Bricolage Grotesque | 22sp / 28 | 700, -0.015em | Detail page titles, month headers |
| Display S | Bricolage Grotesque | 18sp / 24 | 700 | Card titles, section names in the table of contents |
| Body L | Atkinson Hyperlegible | 16sp / 24 | 400 | Note bodies, anything read at length |
| Body M | Atkinson Hyperlegible | 14sp / 21 | 400 | Subtitles, supporting text, list rows |
| Body S | Atkinson Hyperlegible | 13sp / 19 | 400 | Tertiary detail. The floor. |
| Label | Atkinson Hyperlegible | 14sp / 18 | 700 | Buttons, chips, emphasis inside body text |
| Nav label | Atkinson Hyperlegible | 11sp / 14 | 700 | Bottom navigation only. Exempt from the 13sp floor, see below |
| Mono | JetBrains Mono | 11sp / 16 | 400, tracking 0.12em, uppercase | Eyebrow labels, timestamps, counts, metadata. Exempt from the 13sp floor, see below |

**The two exemptions from the 13sp floor, and why they are the only two.** Section 3 item 4 sets a 13sp minimum. The nav label and the Mono metadata style sit below it, and both are deliberate.

Neither ever carries information on its own. A nav label is always paired with an icon and a content description. A Mono eyebrow, timestamp, or count is always directly above or beside the content it labels, and it labels rather than states: removing it would cost context, not meaning. Both are short, and Mono is uppercase and tracked, which raises its cap height and letter distinction well above what 11sp lowercase body text would give.

Both scale with dynamic type like everything else, so a person who has raised their system font size gets them larger, which is the case the floor exists to protect.

Nothing else may be added to this list. If a third candidate appears, that is a sign the layout is too dense, and the layout gets fixed rather than the floor lowered.

**Atkinson Hyperlegible is a deliberate choice, not an aesthetic one.** It was designed by the Braille Institute for maximum character distinction for low-vision readers. The audience for this app is stressed, frequently older, and often reading in bad light. Verify the current release name and license at build time and bundle it.

**Script coverage is a real problem to solve, not a detail.** Bricolage Grotesque and Atkinson Hyperlegible cover Latin. Arabic and Chinese need Noto Sans Arabic and Noto Sans SC, bundled, with a fallback chain that is verified by rendering real strings in all four languages on a device and comparing them side by side. In Arabic and Chinese, display text uses the Noto face at bold weight rather than forcing a Latin display face that has no coverage. Do not assume the system font will cover it. Do not ship a screen where one language renders in a different face than the rest of that language's screen.

### 4.4 Right to left

Arabic ships in v1, so every screen is built direction-aware from the first screen. This is not a late localization pass.

- All layout uses start and end, never left and right.
- Directional icons mirror: chevrons, back arrows, the trail's own direction of travel.
- **The trail mirrors.** The dashed line and its nodes move to the end edge and the content flows from it. A timeline that reads left to right in an RTL layout is broken, not stylish.
- The year scrubber moves to the start edge.
- Numbers, dates, and measurements follow locale conventions, and the mono metadata style must be verified to render correctly in an RTL context rather than reversing.
- Every screen is screenshotted in Arabic during the hardening phase and compared against its English counterpart.

---

## 5. Components

### 5.1 The mark

Two stacked rounded bars in `blaze`, **both the same width**, which is a painted trail blaze. Used at 22dp in the app bar, 44dp on the launch and About screens, and as the app icon on a `paper` field. Never enclosed in a circle, never given a gradient, never animated except the launch fade.

**Proportions:** each bar is 22 wide by 6 tall with capsule ends, and the gap between them is 5. Scale those proportions rather than choosing new values.

**Two corrections on 2026-07-31, both from looking at the built icon rather than at the specification.**

First, this section previously said the upper bar was slightly narrower than the lower. That contradicted `reference/screen-grid.html`, which draws both at `width:22px`, and the difference was not listed in section 3, where every deliberate override of the mockups is required to appear. It was an error in this document rather than an intended override, and it had already been built the wrong way once. **Equal widths, and that is not negotiable.**

Second, the reference draws the bars at 22 by 9 with a 3 gap, and built at icon scale that weight reads as a hamburger menu control. Thinned to 22 by 6 with a 5 gap and capsule rather than cut ends, which reads as brushed on rather than drawn. Listed in section 3 as item 9, because it is a genuine departure from the reference rather than a correction to it.

### 5.2 The trail

The signature element, specified exactly:

- Route line: 2dp stroke, dashed 6dp on and 6dp off, `blaze` at 65% opacity, drawn behind the nodes.
- Node: 12dp circle with a 3dp ring in the current background color, so it reads as sitting on the line rather than beside it. Node color carries the entry type: `blaze` for a call, `blue` for a visit, `alert` for an incident, thread color when filtered to a thread.
- Care thread routes use the thread's own color at the same dash pattern. An ended thread drops to 35% opacity and keeps its color, so it reads as finished rather than deleted.
- In the milestone arc and chapter journey views the line is continuous rather than dashed, with larger 12dp ring nodes, because those are the person's actual path rather than a filter over entries.

### 5.3 Cards

`card` surface, 20dp radius, 16dp padding, soft two-layer shadow in light and surface-step elevation in dark. No border. No colored edge. A card may carry a mono eyebrow label above its content, in `ink3` text-safe, 11sp, uppercase, tracked.

### 5.4 Buttons

Filled: `blue` fill, `on_blue` label, pill, 48dp minimum height, Label type.
Quiet: `card` surface with elevation, `ink` label, same geometry.
Text: no container, `blue` label, still 48dp of touch area.
Destructive: `alert` fill, white label, and only ever inside a confirmation flow, never as a resting state on a screen.

An action keeps the same word through its whole flow. The button that says Export produces a result that says Exported.

### 5.5 Bottom navigation and the capture button

Four destinations, always in this order: Today, Notebook, Projects, More. `card` container, 24dp radius, 8dp inset from the screen edges, elevated. Icon 20dp above an 11sp label. Active state is `blue_deep` on both icon and label, plus the label at weight 700, so color is not the only signal.

The capture button sits in the center of the navigation container, overlapping its top edge by 16dp: 56dp circle, `blaze` fill, dark plus glyph in `onBlaze`, present on every screen in all four tabs. The glyph is deliberately not white, for the contrast reason given in 2.4. It is the only way data enters the app and it never moves, never hides on scroll, and never changes color.

### 5.6 Pills

Small, 11sp Label, fully rounded, tonal background with its matching text color: open uses `alert_soft` with `alert` text, resolved uses `leaf_soft` with green text, active uses `blue_soft` with `blue_deep` text, gold uses `blaze_soft` with gold text. Every pill's text says what the state is. No pill is ever color alone.

### 5.7 Standing instruction tags

Two tags only, and they are load bearing rather than decorative. "Backed by federal rules for nursing homes" uses the blue tonal treatment. "Your request" uses a neutral `sand` treatment. Both are tappable and open the plain explanation of what the difference means, taken verbatim from the template catalog's `standing_instruction_tags` strings. The federal tag must never appear on a notebook whose current chapter is not a nursing home without the explanation being visible, because the backing genuinely does not carry over to assisted living or home care.

### 5.8 Charts

Line for continuous measures, plotted in `blue` at 2.5dp with a 3.5dp end point. Medication start markers are a 1.5dp dashed `blaze` vertical rule with a gold text label. Baseline hairline only, no grid.

**Charts obey the content rules absolutely.** No target band, no normal range, no threshold line, no color coding by value, no red or green points, no arrows, no judgment of any kind. A gap in the data renders as a gap, with the line broken, never interpolated, and never annotated as a lapse. Where a value carries a clinician's assessment, the label says so.

### 5.9 Text fields

**A new pattern, defined here once because nothing existing could carry it.** The reference file shows filled fields inside screens but never specifies the component, and every capture screen needs one. Defined per section 10: stated with its states, then used everywhere the pattern applies. Nothing else may be invented for text entry.

**Geometry.** Full width. `sand` surface, 12dp radius, matching the inset tile. 16dp horizontal and 14dp vertical padding, giving a 48dp minimum height. No border in the resting state, because a 1px gray outline as a field's only definition is on the banned list in section 1. The recessed surface is what says it is a field.

**The label sits above the field,** in Body M, `ink2`, with 8dp between it and the field. Never a floating label that moves on focus, and never a placeholder standing in for a label: a placeholder disappears exactly when the person needs it, which for this audience is the moment they were interrupted.

**Text inside** is Body L in `ink`. The hint is Body L in `ink3` text safe, and it is genuine guidance rather than a repeat of the label. "Whatever you call them is fine" is a hint. "Name" is not.

**States, all of them:**

| State | Treatment |
|---|---|
| Empty | `sand` surface, hint visible |
| Filled | `sand` surface, value in `ink` |
| Focused | 2dp `blue` outline offset 2dp, the same focus treatment as every other focusable thing |
| Disabled | `sand` at reduced opacity, label and text in `ink3` text safe. Rare, because fields in this app are almost never disabled |
| Multi line | Grows with content up to a stated maximum, then scrolls internally. Never a fixed height that clips a long note |

**There is deliberately no error state, and that is a rule rather than an omission.** Every capture field is optional and partial is a finished state. A field cannot be wrong, so it never turns red, never shows a warning glyph, and never blocks saving. Where a value genuinely cannot be interpreted, such as an unparseable date, the app keeps what the person typed and says what it could not read, below the field, in `ink2`. It does not discard the input and it does not scold.

**Optional is stated once per screen, not per field,** because every field is optional and repeating it on each one turns a reassurance into noise.

### 5.10 Empty states

Every list has one, written as an invitation rather than an absence. The Today screen's empty state is a coached three-step list, per the reference file, and its first item is always filling in the Emergency Card, because that is the highest value two minutes a new user can spend.

### 5.11 Choice chips

**A new pattern, defined here once because nothing existing could carry it.** Section 5.6 specifies pills, which report a state and are not touchable. Screen 26 of the reference file needs the opposite: a small control the person taps to choose among a few short answers, for the rough date and for the care thread. Defined per section 10, then used everywhere the pattern applies. Nothing else may be invented for choosing among a few short options.

**Geometry.** Height 40dp with a 48dp touch target, horizontal padding 16dp, fully rounded, laid out in a wrapping row with 8dp between chips and 8dp between rows. Chips wrap rather than scroll horizontally, because a horizontal scroller hides options and the whole point of the set is that the person can see every answer at once.

**States.**

| State | Treatment |
|---|---|
| Unselected | `sand` surface, Body M in `ink` |
| Selected | `blue_soft` surface, Label type at weight 700 in `blue_deep`, plus a 2dp `blue` ring |
| Pressed | Quick 120ms, per section 6 |
| Focused | 2dp `blue` outline offset 2dp, like every other focusable thing |
| Disabled | `sand` at reduced opacity with `ink3` text safe. Rare, because a choice here is never wrong |

**Selection is never carried by color alone.** A selected chip changes weight and gains a ring, so it is distinguishable with color vision differences and in a grayscale screenshot. This is the section 2.2 rule applied to a control rather than to a status.

**A chip may carry a leading dot** in a care thread's route color, 8dp, which is how a thread identifies itself elsewhere. The dot is never the only thing distinguishing two chips, since each chip also carries the thread's name.

**Every chip set includes an answer that means "I do not know".** "Not sure" for a date, "Not sure yet" for a thread. It is a real answer that saves and files, never a way of postponing the question, and it is the reason this pattern exists rather than a required picker.

### 5.12 The icon tile

A rounded tile carrying one line drawing, which is how the reference file draws every row of the table of contents. Radius 12dp, already named in section 4.2 before anything was built against it.

**Geometry.** Tile 36dp, drawing 20dp inside it, centered. The drawings are authored on a 24 unit grid with a 1.7 stroke, round caps and joins, and no fill, which is the grid and weight the reference file's own icons use. Scale the grid rather than redrawing at a new size.

**The drawings are paths, never an icon font.** The same reason the chevron is a path: a font falls back to a box glyph in a language nobody tested, and this app ships in four scripts.

**Fill carries weight, and it is the app's only monochrome emphasis device.**

| State | Treatment |
|---|---|
| Emphasized | `sand` fill, drawing in `ink` |
| Standing | no fill, drawing in `ink2` |
| Quiet | no fill, drawing in `ink3` non-text |
| Emergency Card | `alert_soft` fill, drawing in `alert` text-safe, at every weight, per 2.2 |

**No icon here mirrors.** Section 4.4 mirrors directional icons and none of these is directional. A chevron beside the tile is the thing that flips.

**An icon is never the only thing naming what it sits beside.** Every row carrying a tile also carries the name in words, so the tile is recognition rather than information, and the stroke is held to the 3:1 non-text ratio rather than a text ratio.

### 5.13 The group header

A mono eyebrow with a hairline running out to the end edge, which is how the reference file heads a month in the trail. Used wherever a long list needs to become a few groups.

The label is Mono, uppercased against the catalog's own locale rather than the device's, in `ink3` text-safe. 12dp between the label and the rule, the rule 1dp in `ink3` non-text at 40%. 24dp above the header, 12dp between it and the first row under it, per 4.1.

The label carries no layout weight and the rule carries all of it, so the label takes exactly the width it needs. A label long enough to fill the row, which is what the longest language does, wraps and the rule shrinks to nothing rather than pushing the words off the end edge.

The rule is decorative in the sense 2.3 defines: remove it and nothing becomes unreadable, because the words carry the heading alone.

### 5.14 The press state

**One press treatment for everything the person can touch.** A control that does nothing when pressed reads as broken, and this app is used by someone in a hallway who cannot tell a slow app from a dead one.

**The resting surface moves 8% of the way toward `ink`**, over the quick 120ms of section 6.

That is one rule for both themes and every surface, and it works because `ink` is dark on light paper and light on dark paper. A white card darkens, a dark card lightens, the blue filled button deepens toward `blue_deep`, the gold capture button warms, and a control with no container picks up a faint tint. **No table of exceptions**, which matters more than the exact number: a rule with exceptions is one the next component gets wrong.

An earlier version stepped toward `sand`. It was wrong on anything that was not already a card: on the blue button it pulled the surface toward a warm neutral, which is a different color rather than a pressed one.

Measured on the device in dark theme: a card row goes from (26,36,43) to (43,50,56), the filled button from (127,182,212) to (136,186,214), the capture button from (227,177,85) to (228,182,100).

**Never a bounce and never a scale.** A bounce on every press is banned in section 1, and a scale animation on a card the size of a notebook row reads as a toy.

**Never a ripple on top of it.** The surface is the answer to the touch, and a ripple would be a second, louder answer to the same one. Every control passes `indication = null` and supplies its own `interactionSource`.

**The capture button answers a finger like everything else.** Section 5.5 says it never changes color, and that is about its resting state: it is gold on every screen, always. A press is not a change of color, it is the control saying it heard you, and the one thing data enters through must say that most of all.

**Reduced motion reaches it.** With animations off the step becomes a 100ms fade rather than nothing, so the acknowledgment survives even when the movement does not.

**Focus is a peer of press, not a substitute.** 2dp `blue` at the control's own radius, faded in over the same 120ms, which is the focus treatment 5.9 and 5.11 already name. Press and focus are separate states and a control shows both.

### 5.15 The pinned action footer

**A named layout, because it was built four times and got the same detail wrong three of them.** The disclaimer gate, essentials first setup, the situation picker, and the capture form all have the same shape: content that scrolls, and one or two actions that do not.

**The actions never scroll.** That is what keeps the primary action in the lower half where a thumb reaches it on a large phone, per section 9, whatever the longest language or the largest font size does to the content above it. Putting the actions inside the scroll puts them in the upper third on a short screen and off the bottom on a long one.

**There is always a gap of at least 16dp between the scrolling area and the first action.** Without it the content at the scroll edge ends flush against the action and reads as the action sitting on top of it, rather than as content scrolling behind. **This is invisible at the default font size on a tall screen and obvious at font scale 2.0 or with the keyboard up**, which is where all three instances of it were found. See D38.

**The secondary action sits below the primary at equal reach**, with no styling that makes it feel like giving up. Skipping setup and answering "Not sure yet" are real paths.

**Content clipped at the scroll edge is correct and is not this defect.** A list has to end somewhere. The defect is specifically the absence of separation between the two regions.

### 5.16 The date picker

**A new pattern, defined here once because nothing existing could carry it.** Chips answer "roughly when" in one tap and cannot answer "the fourteenth, at about two". Section 10.9 requires an exact date and time to be a peer of the chips rather than something behind them, and it requires a person to be able to say a month or a year without doing arithmetic. No combination of chips, fields, and cards does that.

**It is one sheet with three levels of precision, not three controls.** A day, a month, or a year, each optional, each mapping to exactly one EDTF form. The person picks how much they know and the app records that and no more. Putting them in one place is what makes "I only know the month" as easy as "I know the day", which is the whole point: the coarse answer must not feel like the failure case.

**Geometry.** A bottom sheet, 28dp top corners per 4.2, opened with the standard spring rather than the expressive one, since choosing a date is not one of the three moments 5.5 reserves overshoot for.

**The three levels, in this order:**

| Level | Control | Records |
|---|---|---|
| A day | A month grid, seven columns, one cell per day | `2024-11-18` |
| A time, optional, only once a day is chosen | An hour and minute row | `2024-11-18T14:40` |
| A month | Twelve chips, plus the year stepper | `2024-11` |
| A year | The year stepper alone | `2024` |

**Day cells** are 40dp circles inside a 48dp touch target, per section 3 item 5. Unselected is bare, selected is `blue_soft` with a 2dp `blue` ring and the label at weight 700, which is the choice chip treatment from 5.11 in a round shape rather than a second selection language. Today carries a small `ink3` dot beneath it and is never preselected.

**A day outside the current month is not shown**, rather than shown grayed. A grayed cell that cannot be tapped is a control that does nothing, which D42 removed elsewhere for the same reason.

**Nothing is preselected and the sheet opens on whatever the entry already says.** Opening it on an entry dated "sometime in November" opens November with no day chosen, so confirming without touching anything changes nothing. **A picker that preselects today turns every mistap into a claim.**

**The time is optional and separate.** A day with no time is a day, and adding a time is a second act. This is the one place the model's precision is visible as a choice, and it is visible as "do you know the time" rather than as a precision selector.

**States.**

| State | Treatment |
|---|---|
| Nothing chosen | No cell selected, the confirm action still available, and it answers "not sure" |
| A day chosen | That cell selected, the time row appears |
| A month chosen | The month chip selected, the day grid dimmed to show it is no longer the answer |
| A year chosen | The year alone, both the month chips and the grid unselected |
| Editing an existing date | Opens on that date at that precision, nothing else changed |

**Every level has a way back to less precision**, because a person who taps a day and then realizes they are not sure must be able to say so without leaving and starting again.

**It never shows EDTF, a precision name, or a format.** Section 10.9. What it shows is a calendar, twelve month names, and a year.

---

## 6. Motion

Two spring personalities and three durations, per the universal standards.

**Standard spring**, everything by default: stiffness 380, damping ratio 0.9, no overshoot. Screen transitions, sheet presentation, list item entry, expansion.

**Expressive spring**, slight overshoot, stiffness 300, damping ratio 0.68. Reserved for exactly three moments and nothing else: the capture sheet opening, a milestone being added to the arc, and an incident being marked resolved. Three moments, because each one is a small piece of relief in an app used during hard times.

**Durations**: quick 120ms for press feedback and chip selection, standard 240ms for sheets and navigation, deliberate 400ms for the trail drawing itself in on first view of a timeline.

The trail draw is the one ambient flourish: on first entry to a timeline the dashed line strokes in from the top over 400ms and the nodes fade in behind it, staggered 30ms apart. It happens once per screen entry, never on scroll, and never repeats while the user is reading.

**Reduced motion**, when the system setting is on: every spring becomes an instant state change, the trail draw becomes an immediate render, and the only remaining transition is a 100ms opacity fade. Verify this by actually enabling the setting, not by reading the code.

**Motion carries meaning here or it does not ship.** Identical fade-ins applied to everything read as generated and are on the ban list in section 1. Every spec comes from the tokens above through `LocalMotion`, never built inline, because a spec built inline is one the reduced motion setting cannot reach.

**Everything the person touches responds**, including on screens already built. Section 5.14 defines the press state once for the whole app. Specifically:

- Every button, row, chip, and tappable card has a visible press state.
- Selection is immediate and obvious.
- Expanding a folded section animates rather than snapping.
- Saving an entry animates it into place, so the person sees where it went.
- Sheets rise. They do not appear.

---

## 7. Voice

Write like a person explaining something to a friend across a table. Plain words, short sentences, contractions welcome.

**Never**: exclamation points, hype words, fear language, apologies for the software's own limits, sentences that could appear in a generic tech advertisement, or anything that congratulates the user on using the app.

**No em dashes** in anything a person reads: app copy, documentation, README, commit messages, and store text. Commas, periods, and colons instead. Source code is exempt where the character is functionally required, for example inside a regular expression, a test fixture, or data being parsed. The rule is about what users and readers see, not about the bytes in a source file.

**Specific rules for this app:**

- **Never imply a lapse is a failure.** "Since you were last here" is correct. "You have not logged anything in 3 weeks" is banned. There are no streaks, no completion percentages on the person's own diligence, no catch-up prompts, and no reminders to use the app.
- **Never interpret.** The app says what it counted and stops. "3 of the 5 resolved incidents involved the evening shift" is correct, and it is followed by a plain line saying that what it means is the person's to judge. "This suggests a pattern of neglect" is banned in every form.
- **Never advise.** No medical guidance, no legal guidance, no "you should," no "consider asking." The templates offer administrative actions, phrased as things families do, never as recommendations.
- **Honest limits, at the moment they matter.** The medications screen says plainly that the app keeps the record and does not remind or alert. The web version says plainly that browsers can clear local data. Say it where the person is, not buried in a settings page.
- **Second person, warm, never familiar.** "Write down the direct number for the unit." Not "Let's write down" and not "Don't forget to."
- **Spanish uses formal usted throughout**, and elders are addressed as Señor or Señora. Chinese and Arabic copy must be reviewed by a native speaker who has dealt with the American care system, not only a fluent translator.
- **Three terms must never be translated with their direct cognate**, because the cognate misleads or stigmatizes: hospice, power of attorney, and social worker. Use the descriptive phrasing plus a plain gloss carried in the template data's `localization_note` fields.

### The disclaimer, final wording

Shown on first launch, before any part of the app is usable, with an explicit accept.

**Rewritten on 2026-07-31, and the reason is worth keeping.** The first wording was a correct disclaimer and a bad first screen. It opened with what the app is not, stacked four denials in a row, and read as though it were protecting the software from the person rather than being straight with someone about to trust it with something that matters. It disclosed everything it needed to and made a person in a hallway feel handled.

**The safety substance did not change and may not.** Everything the old wording covered about what this app is and is not is still covered: that this is a record keeping app, that it is not a medical app, that it gives no medical advice, that nothing here replaces a doctor, a nurse, emergency services, or a lawyer, that urgent situations mean calling emergency services, and that what is written stays on the phone. **None of that may be cut on the grounds of warmth.**

**One line was cut, on the owner's decision, and it is not to be restored.** The old third paragraph ended "and you are responsible for what you write down". The owner read the rebuilt screen and said plainly that there is no reason to be that aggressive, and that there are friendlier ways to say things. He is right, and the line was doing nothing the rest of the screen does not already do: the app already says it never decides what anything means, which is the honest part. The rest was the software bracing against the person on the first screen they ever see. The third block now says that the record is theirs and that they choose what goes in it.

This is a decision rather than a drift, so it is recorded as D32 and stated here, because the paragraph above says nothing may be cut and a future session acting on that in good faith would put it back.

The wording, carried verbatim rather than paraphrased:

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

**Structure is part of the wording.** The three bold lines are section headings in Display S, not emphasis inside a paragraph, and each is followed by its own short body. A person can read one block, look up at a nurse, and come back without losing their place. Built as a single wall of text it was skipped rather than read, which is a failure of the screen rather than of the reader.

The same substance appears in the About screen and the store listing. It is not shown again after acceptance. No version of the app skips it.

---

## 8. Screens

`reference/screen-grid.html` is the layout specification. Build each screen against it. This section states what each screen must do that the image cannot show, grouped as the reference groups them.

**One way in.** Today is the dashboard: the digest headed "since you were last here," open-item counts for incidents, questions, and waiting-ons, the next appointment with its prep status, and the Emergency Card one tap away. Universal search sits at the top. The capture button opens a sheet offering six inputs, each of which files itself: call, visit, incident, measurement, question, document. The person chooses what happened, never where it goes.

**Where everything lives.** Notebook opens to a table of contents with live counts, and the sections never move: care team, medications, appointments, chapters, care threads, the trail, progress, documents, money, standing instructions, ask next time, emergency card. Which sections sit expanded versus folded comes from the active situation template. More holds the person switcher, family update, search, templates, export, situation change, archived trails, support, and about.

**Everything connects.** Every detail page assembles its own relationships. A person's page pulls every call, visit, and prescription involving them plus the questions waiting for them. A medication's page shows its pending question, its incidents, its dose history, and its presence on the Emergency Card. A single trail entry shows every road out of it, with the linked things underlined and tappable. **No dead ends** is the rule: the person never has to remember where something was filed.

**Zooming out.** The trail carries a year scrubber on the end edge. Recent months show entries; older months fold to a one-line summary and open on tap. Month review composes a deterministic summary where every line taps through to its source entry. The milestone arc shows the whole path on one continuous trail.

**Zooming in.** An incident is a thread from report to resolution, with each contact and escalation as a node, exportable on its own. Incidents over time shows the history with a deterministic count above it, and the count is never interpreted. Any single day can be reconstructed, including what the medications and measurements were at that time.

**Search, everywhere.** Universal search from Today returns results grouped by kind. Every section has its own scoped search, showing a chip that says what it is searching and offering one tap to widen. Every result carries its chapter, so the person always knows where in the journey it happened. From any result, one tap assembles everything connected to it into a single view that exports as one document.

**The journey.** Chapters are places, shown as stops on the trail. Inside a chapter: its dates, why the stay began, its incidents, its documents, its care team archived but still searchable, and any project that started there. A medication's journey crosses chapters and keeps its concern flags attached forever.

**Money and the rules.** Bills carry states: needs attention, disputed, waiting on insurance, paid, with totals above. A bill links to its chapter, the call where it was disputed, and any standing instruction it broke. Standing instructions record what was asked, of whom, when, how it was acknowledged, and every documented violation. Running cost sheets serve any long expense, not only facilities.

### Screens built without a mockup

Every screen here was composed from the existing components under section 10 rather than designed, and each has an open `needs-design-review` issue carrying a device screenshot. This list is empty until the first one is built. `HANDOFF.md` carries the same list so a review can be done in one sitting.

**The disclaimer gate.** The first screen anyone sees, and a gate rather than a notice: nothing else is reachable until it is explicitly accepted, and the acceptance is recorded with a timestamp so it is shown exactly once per install.

The wording is fixed by section 7 and carried verbatim, not paraphrased. Composed from the mark at 44dp, Display L for the heading, Body L for the lead, the card from section 5.3 with Display S and Body M inside it, and one filled button. Nothing new was introduced.

**Rebuilt on 2026-07-31 under the standard in section 10.5.** The first version was one heading and two long paragraphs of Body L. It disclosed everything it had to and read as a wall of text, which is a screen a person scrolls past to reach the button. The three things someone actually has to take away are now three cards, each with its own heading, so the screen can be read in pieces by a person who keeps getting interrupted. Nothing was cut to get there, and section 7 states that as a constraint on any future edit.

The three headings are real headings to a screen reader, not bold text, so traversal by heading works and matches the visual order.

The text scrolls and the action does not. That is what keeps the accept button in the lower half where a person holding a large phone in one hand can reach it, per section 9, while letting the wording grow to any font size or translation length without pushing the action off the bottom. Building it with the whole column scrolling put the button in the upper third, which is why it is built this way.

The mark carries no content description here. The heading immediately below it already says where the person is, and a screen reader announcing a logo before every title is noise.

Tracked on issue #28.

**Essentials first setup.** Asks three things and lets everything else wait: who you are looking after, where they are, and one phone number worth having in a hurry.

Every field is optional, including the name, and continuing with all of them blank is a real path that produces a working notebook. There is no required field marker, no validation, no error state, and no progress indicator, because a progress indicator on setup frames an unfinished form as a deficiency.

One screen rather than a three step wizard. A wizard means three taps before anything is written down and hides how little is being asked. One short scrolling screen shows the whole ask at once, which is what makes it possible to see that it is nearly nothing.

**Rebuilt on 2026-08-01 under the standard in section 10.5.** The first version was five labeled boxes in a row, four of them empty and unhinted, under a mono "Optional". It worked and it read as paperwork, on the screen that decides whether someone in a hallway keeps going.

**The reassurance is one warm sentence, once.** "Answer what you can and leave the rest. None of it is required, and you can change any of it later." It replaced the word Optional, which is accurate and is the vocabulary of a form being administered, and it carries the second half a label could not: that nothing here is permanent.

**Three groups, headed with the group header from 5.13**, the same one the notebook and the situation picker use, so a person arriving from the disclaimer meets one app rather than three. Who, where, in a hurry.

**Every field carries a hint that is genuine guidance**, per 5.9, never a repeat of the label. "Mom, Dad, my aunt, whatever fits" is a hint. "Who they are to you" is a label. That single change did more for how this screen feels than the grouping did.

**Group headings and field labels are never the same words.** Built with them shared, the screen showed the same sentence twice in a row and a screen reader announced it twice. `SetupFlowTest` asserts it now, because the next person editing the copy will not know.

**The questions scroll and the actions do not**, which keeps Continue in the lower half where a thumb reaches it, per section 9, whatever the font size or translation length does to the questions above. There is a real gap between the last question and the button: with the keyboard up the scrolling area shrinks until the field at its edge sits against the button, and on the phone that read as an overlap rather than as content scrolling behind. Invisible in a resting screenshot and obvious in a hand.

Skipping sits below Continue at equal reach with no styling that makes it feel like giving up.

Composed from Display L, Body M, the group header from 5.13, the text field from 5.9, the press state from 5.14, one filled button, and one text action. Tracked on issues #30 and #37.

**The situation picker.** Fourteen care settings, grouped by where the care is happening, each carrying its name and the subtitle that tells two similar settings apart.

**Rebuilt on 2026-08-01 under the standard in section 10.5.** The first version listed all fourteen flat, at one weight. It worked and it was a wall, on the first real screen after the disclaimer, in front of someone in a hallway deciding whether to keep going.

**Grouped by where the care is happening**, which is the one thing that person already knows: in a facility, at home, ongoing treatment, comfort focused care. **The membership is in the catalog, not in the app.** `templates/data/situations.json` carries a `group` on every template, so the web version groups identically rather than reimplementing the list and drifting from it. A setting whose group this version does not recognize still renders, under no heading, because a person must never fail to find their own situation because of a data edit.

**Ordered by how many caregivers each setting covers**, which is what the catalog's `phase` already marks. The common ones lead, inside every heading.

**The subtitle is never dropped.** A nursing home and assisted living are one word apart on this screen and are not the same thing.

**The burden line appears on the settings that lead their group, and nowhere else.** It is one sentence naming what is hard about a setting, written so the person feels understood rather than processed. Read once it does that. Read fourteen times in a row it is a wall of other people's hardship. `templates/SCHEMA.md` says to use it as supporting text where it helps, and this is that taken at its word. It also gives each group the same shape the notebook has: the likeliest option is the fullest row.

**"Nothing here is permanent" is pinned above the skip action**, with the list scrolling behind it, because a sentence that removes the pressure from a screen is worth nothing if it takes fourteen options of scrolling to reach.

No chevron on a row. A chevron implies going somewhere to look, and tapping here chooses.

Composed from Display L, Body M, Body S, the card from section 5.3, the group header from 5.13, the press state from 5.14, and one text action. Tracked on issues #32 and #41.

**The capture form.** One form for four of the six capture inputs: a call, a visit, an incident, and a question. They record the same four things and differ only in wording, so the shape is defined once and the words come from the catalog per kind. Which four is declared in one place, exhaustively, so a seventh capture kind cannot be added without deciding whether it belongs here. Measurement and document are genuinely different shapes and get their own screens.

**This is screen 26 of the reference file rather than an undrawn screen,** which is why it is listed here as a correction. It was built first as two single line text fields, which worked and was not what was designed. Rebuilt on 2026-07-31 to the mockup: rough date chips with "Not sure" among them, care thread chips with "Not sure yet" among them, an open note area, and a save action reading "Save what you have".

Everything is optional and saving with nothing touched is allowed, because a person who hangs up and taps the gold button has already done the useful thing, which is recording that something happened and roughly when. The note grows with what is typed, since a fixed height silently teaches people to write less.

**"Not sure" stores no time at all,** with a precision of unknown, rather than today's date with a shrug attached. Every screen downstream then renders it as not known, which is the truth.

**The thread question defaults to not knowing,** which sends the entry to the Unfiled tray, and the screen says so underneath while the person can still change it. The thread question is not asked at all on a notebook that has no threads, because a question whose only answer is "not sure yet" is not a question.

Composed from Display L, Body M, Body S, the text field from section 5.9, the choice chip from section 5.11, one filled button, and one text action. Tracked on issue #34.

**The notebook table of contents.** Twelve sections in four labeled groups, each row carrying an icon tile, its name, and one live count. **Drawn in the reference as screen 04**, and listed here because the built screen departs from it in four ways, all of them recorded in section 3 item 8.

**Rebuilt on 2026-07-31 under the standard in section 10.5.** The first version rendered twelve identical cards. It worked, and it read as a list of everything rather than as a table of contents, which is what the owner named.

**The order never changes and nothing is ever hidden.** The groups are placed at the three points in the existing order where the subject changes, so the grouping adds structure without rearranging anything. The situation template's `forward` and `folded` arrays decide weight only, per D33: a forward section gets the fullest row, a folded one collapses to a single line in its own place, and everything else sits between them. A folded section is one tap from where it always was.

**One count treatment at every weight.** Same Mono style, same `ink3` text-safe, whether the row is forward, standing, or folded. The row's emphasis says what this care setting tends to need, and it must never be mistaken for a judgment about how full the section is.

**A count of zero reads as words.** "Nothing yet" invites where a column of zeros reads as a scorecard, and this app never keeps score of anyone's diligence. That makes the empty state the resting state: a new notebook is twelve rows each saying "Nothing yet", which is a complete screen rather than a blank one, and there is no separate empty layout to fall into.

**A notebook with no situation template renders every section at the standing weight.** "Not sure yet" is a real answer to the situation picker and it must not cost anyone a working notebook.

Composed from Display L, Body M, Display S, Label, the Mono count style, cards from 5.3, the icon tile from 5.12, the group header from 5.13, and the press state from 5.14. Four of the twelve icons had no drawing anywhere in the reference and were composed on the same 24 unit grid at the same weight, listed in the source. Tracked on issue #36.

**Today, the empty state.** A coached three-step list, first item the Emergency Card.

**Built because persona P1 required it and was not getting it.** P1 is the person in a corridor on the day of an admission, and one of the five things that must be true for them is exactly this. It was the not-built screen, which is the wrong thing to hand somebody at the moment they are most likely to put the phone away.

**The three steps are guidance, not controls.** Two of them lead to screens that do not exist yet, and offering those as buttons would be the dead end D44 removed from the capture sheet. The one thing a person can act on now, capture, is already the gold button on every screen, so the list reads as what to do next rather than three disabled offers.

**The numbering is allowed here**, and section 1 bans numbered markers only where the content is not genuinely a sequence. Filling in the emergency card first is the entire point of the list.

**The coaching stays until the emergency card exists, not until something is written.** Tying it to whether anything had been written took the most useful two minutes in the app off the screen the moment somebody logged their first call, which is the opposite of the intended reward. Both the digest note and the coaching appear together when both apply, and the coached list changes its heading from "Nothing written down yet" to "Worth doing next", because a heading that contradicts the screen under it reads as a bug.

**A notebook with something in it says the digest is still being built, and says nothing is waiting on it.** The second half matters more: a person reading that a summary is coming needs to know in the same breath that their records are kept regardless, or the sentence reads as a reason to stop writing things down. That branch carries `ShellTags.NOT_BUILT` so it cannot survive to release.

`TodayScreenTest` asserts the empty state never scolds and never keeps score, which is rule 13 checked rather than remembered.

Composed from Display L, Display S, Body L, Body M, the Mono style, and cards 5.3. Tracked on issue #78.

**Adding a measurement.** The fifth of the six capture inputs, and the one with the sharpest content rule attached to it.

**It does not fit the shared form, which is why it has its own screen.** The other four record that something happened and what was said. This one records a value, and a value needs to know what is being measured before anything else on the screen means anything.

**Two steps, and the first one is a real question.** Choosing what you are tracking is not a wizard step in front of the real form, it is the first thing anyone recording a measurement has already decided. Things the notebook already tracks come first as chips, so after the first week the answer costs one tap. The sixteen presets sit below under their own heading as short rows, and a preset already being tracked is not offered twice.

**A measure is created the first time someone records one, never at setup.** A notebook that arrives with sixteen empty charts is a list of things somebody has not done, which is the scorecard this app does not keep.

**A number and words are different things.** Weight is a number. "Ate about half her lunch" is not, and forcing it into one would either lose it or invent a figure nobody gave. The preset's own style says which, and the field and the keyboard follow.

**Nothing is judged.** No range, no normal value, no threshold, no color by value, no arrow, no comparison to last time, per rule 2 and section 5.8. The screen says plainly that the app writes the number down and does not tell anyone what it means, which is the honest limit stated where the person is rather than buried in a settings page. `MeasurementTest` asserts that no judgment word appears in any of this screen's copy.

**The preset's `advice_risk` never reaches the person.** It exists so the rendering layer can be careful about how it displays a value, and it must never become a caution, because a caution is the app judging.

The cadence line under each preset says how often families typically record it. **It is guidance about the shape of the thing, never a schedule anyone is held to**, and nothing here ever reminds or alerts.

Composed from Display L, Body M, Body S, the group header 5.13, the text field 5.9, choice chips 5.11, the pinned action footer 5.15, one filled button, and one text action. Tracked on issue #42.

**The date picker.** One sheet with three levels of precision, opened from a chip that sits among the rough date chips rather than behind them.

**Built because section 10.9 requires an exact date and time to be a peer of the chips.** Someone logging a call from three months ago, or who knows the minute, is a normal case rather than an edge one, and making them exhaust the chips first would say otherwise.

**It is one control, not three.** A day, a month, or a year, each optional. Putting them in one place is what makes "I only know the month" as easy as "I know the day", which matters because for a record written from memory the coarse answer is usually the true one. The whole month sits as a chip under the grid, in words, so choosing it is choosing rather than giving up.

**Nothing is preselected, and it opens on whatever the entry already says.** Confirming without touching anything changes nothing. A picker that preselects today turns every mistap into a claim, in a record somebody may rely on years later. Today carries a dot so a person can orient, and never a selection.

**The time is a second act.** It appears only once a day is chosen, and it reads as "do you know the time" rather than as a precision selector. A day with no time is a day.

**What was chosen is read back in words underneath the chips**, through the same renderer every other date goes through, so the person sees the claim they are about to make rather than a control state.

Specified in full at 5.16. Tracked on issue #39.

**The Unfiled tray.** Everything the person saved without saying where it belonged.

**Built because the capture form already promised it.** The form tells the person their entry is going to the Unfiled tray, and until 2026-08-01 there was nowhere to see it. A promise the app makes and does not keep costs more than a feature it never mentioned, which is why this outranked the rest of the queue.

**It is not a thirteenth notebook section and never becomes one.** The twelve are fixed. This is a thing waiting for the person rather than a place they filed something, so it is reached from a card at the top of the notebook that **appears only when something is actually waiting.** When the tray is empty there is nothing to find, so there is nothing to show and no empty room to walk into. The card uses `blaze_soft`, which 2.2 allows as a gold tonal waiting-on background and which is not the accent, and it carries a word as well as a color.

**The app suggests and the person confirms.** A home is suggested by plain word matching, per `MASTER_SPEC.md` section 4.2, and it arrives already selected, which is what makes confirming it one tap. Changing it is one more. **Nothing is written until the person taps the action**, and the app never files anything on its own.

**The suggestion is allowed to find nothing, and often will.** An entry reached this tray because it was hard to place, and a wrong guess presented confidently is worse than an honest blank. Two equally good guesses also produce nothing, because that is what the app knowing means here.

**"None of these" is a real answer**, not a way out of the question. It clears the entry without a thread, because the tray holds things nobody has looked at yet rather than things without a thread.

**It asks the same question the capture form asks, in the same words and the same control.** The person is here because they did not answer it there. Asking it differently would make it a second question rather than the same one, still open.

**The empty state is the common one**, since most notebooks will have an empty tray most of the time, and it reads as nothing waiting rather than as an absence.

Composed from Display L, Body M, the Mono metadata style, cards 5.3, choice chips 5.11, the pinned action footer 5.15, and one text action. Tracked on issue #53.

**Many things at once.** Care threads are parallel streams, each identified by a dashed route in its own color. The trail filters to any single thread. Ended threads keep their whole story. Capture forgives: every field optional, rough dates allowed, and anything the person could not categorize lands in an Unfiled tray where the app suggests a home by plain word matching and the person confirms. **The app never files anything on its own.**

---

## 9. Accessibility floor

Not a phase. A gate on every screen.

- 48dp minimum touch target everywhere, including rows that look smaller.
- Visible focus state on every focusable element, using a 2dp `blue` outline offset 2dp.
- Complete screen reader labels on every control, including the capture button, every chart, every pill, and every gesture-revealed action. A chart's label reads the measure, the range of dates, the latest value, and the presence of any gap. A pill's label reads its state as a word.
- Contrast meeting AA in both themes at real sizes, measured and recorded.
- Dynamic type respected up to the largest system setting without clipping, overlap, or lost actions. Test at the maximum, not one step up.
- Reduced motion respected, verified with the setting on.
- Every screen operable one-handed on a large phone, with primary actions in the lower half.
- No information carried by color alone, anywhere.
- Screen reader traversal order matches visual order on every screen, verified with the reader on.

**Two additions that follow from this audience, added 2026-07-31 with D34.**

- **Anything gesture-only also has a visible, non-gesture path.** A swipe action nobody discovers is a feature nobody has, and this audience is not exploring the interface for pleasure.
- **Nothing important sits where a one-handed thumb cannot reach it on a large phone.** That is the actual holding position this app is used in, so it is a layout constraint rather than a nicety.

**Date controls specifically** must be fully operable by screen reader and at maximum font size, and a screen reader must read an imprecise date as the person expressed it rather than as a resolved timestamp. See 10.9.

**Every touchable node says what it is, and the build checks it.** `ScreenReaderTest` walks every screen's semantics tree, in every window including a sheet's own, and fails on any node carrying a click that has neither text nor a content description. It found one on the first run: the capture sheet's drag handle, an unlabeled button on the screen every piece of data enters through.

That half is automated. **It does not replace running the reader by hand**, because traversal order and how a label actually sounds still need ears.

**A control that does nothing is removed, not labeled.** The drag handle had no state to toggle, so naming it would have put a control in a reader user's path that does nothing they can use, which is worse than an honest absence.

**Verified means with the setting on.** The reader running, the font at its maximum, reduced motion actually enabled, on the phone. Reading the code proves nothing here, and every one of these has a way of passing in the editor and failing in a hand.

---

## 10. Screens that were never drawn

The 27 screens in `reference/screen-grid.html` do not cover everything this app needs. The template library, the template pickers, the template editor, and a number of sub-screens have no mockup. You will reach them. The order of the rules below matters.

### 10.1 Do not stop, and do not ask

Build the screen. Then log it. Both parts are required and neither substitutes for the other.

### 10.2 Compose, do not design

`DESIGN.md` and the screen grid together already define a finished design language: a card, a section header, a list row with its optional subtitle and chevron, an eyebrow label, a pill, an empty state, the spacing scale, the type scale, and the motion vocabulary. An undesigned screen is **assembled from those pieces**.

It is not an opportunity to introduce a new component, a new layout idiom, a new interaction pattern, or a new way of presenting a kind of information the app already presents somewhere else.

**The test:** if you find yourself designing, you have already gone wrong. You should be composing.

**When a genuinely new pattern is unavoidable,** because nothing existing can carry the content, define it once in section 5 with its states and its rules, then use that definition everywhere the pattern applies. **A pattern that appears twice in two different forms is a defect,** and the fix is to correct the earlier one rather than leave both standing.

### 10.3 Complete means complete

Every screen ships complete whether or not it was mocked up. Complete means all of these, not most of them:

- the empty state
- the one-item state
- the many-item state
- the partially-filled state
- the long-text state, and the longest-language state, which is usually German-length wrapping in Spanish or a long Arabic string
- the loading state
- the error state
- right to left

**A screen without its empty state is not built.** The person must never encounter a blank area, a placeholder string, a stub, a debug label, a truncation, or a layout that only holds together because the sample data happened to be tidy.

### 10.4 Log it in three places, immediately

At the moment the screen is built, not at a phase gate. A screen built on Tuesday and logged on Friday is three days of work built on top of an unreviewed decision.

1. **An issue labeled `needs-design-review`.** The body says which screen, why it was needed, which existing components it was composed from, what you deliberately did not invent, what you were unsure about, and carries a real screenshot captured from the device. This is the owner's review queue, so it has to be reviewable without reading code.
2. **An entry in section 8 of this document,** describing what the screen does and how it behaves, so this document keeps describing the app as it actually is rather than only the parts drawn in advance.
3. **A line in the running list in `HANDOFF.md`,** so the review can be done in one sitting rather than archaeologically.

### 10.5 Functionally correct is not done

**A screen that works but looks unfinished is unfinished.** This is the standard for every screen, drawn or undrawn, and it exists because four screens were built correct and left visually thin, and every screen after them would have inherited the same bar.

Done means it looks and reads like the rest of the app, it has been looked at on the device, and nothing on it stands in for a design decision that was never made. Specifically, none of the following ship:

- Bare text where the design language has a component.
- A single line text field where the screen grid shows chips, segments, or a structured layout.
- A flat list of everything at once where grouping, hierarchy, or progressive disclosure is called for.
- Uniform visual weight, where a person cannot tell what matters.
- Spacing, type scale, or grouping improvised per screen rather than taken from section 4.

**The fix for a thin screen is always the components that already exist,** never a new one. This is section 10.2 again, from the other direction: composing badly looks the same as not composing.

### 10.6 The checklist a screen passes before its issue closes

Checked rather than remembered, in this order. A screen closes only after every line is true.

1. **Built, installed to the phone over ADB, opened, and looked at on the real device.** Not a preview, not the layout inspector, the actual screen in a hand. Two real bugs in one increment were invisible in review and obvious on the device, recorded as D28.

   **With the keyboard up, on any screen carrying a text field.** That is the state the person actually spends their time in, and nothing else in this project tests it. Two more defects were found that way and only that way, recorded as D38: an action button sitting on top of the last field, and the field sliced through the middle of its box at the scroll boundary. Both looked correct at rest.
2. **It uses the components the design language already has,** and introduced nothing new. If something new was genuinely unavoidable, it is specified in section 5 first, with its states.
3. **Hierarchy is visible at a glance.** A person can tell what matters without reading every word.
4. **It holds up with real content,** not only with tidy sample data. Long names, empty sections, one item, many items, a note nobody bothered to punctuate.
5. **It holds up in the longest language,** which is usually Spanish wrapping or a long Arabic string, and it holds up right to left.
6. **It reads as the same app as the screen before it.** Same spacing, same type scale, same voice.
7. **Every state in section 10.3 exists**, including the empty one.
8. **A screenshot from the device is committed,** and the undrawn screen is logged in the three places section 10.4 names.

### 10.7 Discoverability is part of the screen

Every section, template, and feature must be reachable and, more importantly, **discoverable by someone who does not already know it exists**. A capability that can only be found by a person who already knew to look for it is not finished.

Where something is genuinely hard to surface without cluttering a screen, build it, note the problem on that screen's `needs-design-review` issue, and keep going.

### 10.8 The hierarchy sequence, applied to every screen in this order

Three screens were cluttered at once and they shared one cause: everything presented at the same visual weight, so the person had to read all of it to find any of it. **Uniform weight is not neutral.** It pushes the entire job of sorting onto someone already exhausted.

The fix is the same sequence every time, and the order matters.

1. **Decide what matters most on this screen.** One thing, named out loud, before any layout happens.
2. **Give that one thing the most weight**, through size, position, and the space around it. **Not through color**, which section 2.2 has already spent on actions, the trail, and the Emergency Card.
3. **Group what belongs together** and put a quiet mono eyebrow on each group, per 5.13.
4. **Let the rest recede** to secondary type rather than deleting it.
5. **Then give it room.** Whitespace is what makes a dense screen readable, and it is the first thing sacrificed when a screen is built to be merely correct.

**Grouping adds structure around an existing order rather than rearranging it**, wherever the order is something the person has learned. The notebook is the worked example: twelve sections got four headers and not one of them moved.

**Progressive disclosure is part of hierarchy, not a separate feature.** Everything visible at once is the most common structural tell on the ban list, and it is what made the notebook cluttered.

**Polish applied to a cluttered screen is still a cluttered screen.** Structure first: clear hierarchy, sensible grouping, an obvious next action, and a person who can always find what they entered.

**You have real latitude inside the vocabulary.** Spacing, grouping, emphasis, how a list is organized, how an empty state is worded, where a link belongs. Use the range the design language gives rather than reaching for the plainest arrangement that satisfies the spec. **Consistency is the constraint, not sameness:** screens differ because their content differs, and they should never feel like they came from different people.

### 10.9 Dates, which the person never has to understand

The storage model is `contract/DATA-CONTRACT.md`. This is what reaches the screen, and the whole point is that none of the model does.

**The person never sees EDTF, never sees a precision selector, and never chooses a storage format.** They see chips for the common cases, an exact date and time always available without leaving the flow, and natural expression where it is genuinely easier.

**The exact date and time is a peer of the chips, not something behind them.** Someone logging an event from three months ago, or who knows the exact minute, is a normal case rather than an edge one.

**Whatever the person expresses is recorded at exactly that precision and no finer.** A month stays a month.

**Display never invents precision.** "Sometime in November 2024" is honest. "November 1, 2024" for that same input is a fabrication. This holds in the trail, month reviews, exports, PDFs, and the engine's composed sentences, and composed sentences handle imprecise dates through the message template system in all four locales rather than by concatenating a formatted date into a sentence.

**Unknown is a first-class value.** An entry with an unknown date saves, is valid, and appears in the trail. It is never blocked, never hidden, and never quietly filled in with today.

**Every date is editable forever, from the entry itself, with the same control.** Editing a date never creates a new entry and never loses the entry's links.

**Imprecise entries never disappear.** They sort sensibly among precise ones and appear in a date-range search whenever their range overlaps the query. Filtering by a month returns everything that could have happened in that month, and the app never quietly excludes an entry because it was unsure.

**Charts keep 5.8 exactly.** An imprecise measurement date is plotted honestly or shown as a gap, never interpolated, and never presented as more certain than it is.

### 10.10 Taps are the currency

Someone doing this in a hallway will abandon a flow that takes four taps when it should take two. Reducing them is a design requirement, not an optimization.

**No dead ends.** Every item links to everything it touches, and the person never has to remember where something was filed. **If A shows B, then B shows A.** Build both directions at the time, every time. A one-way link is a dead end wearing a disguise.

**Carry context forward instead of asking again.** Capture opened from a person's page already knows the person. Capture opened inside a chapter already knows the chapter. A question created during an appointment is already attached to it. **Every prefill is a default the person can change, never a decision made for them.**

**Offer what is likely before what is complete.** Recently used and currently relevant entities come before a full alphabetical list.

**Ask the question on every screen as you build it:** what does the person most likely want to do next from here, and is it reachable in one tap. What that turns up becomes its own issue with acceptance criteria, opened then rather than remembered.

---

## 11. Keeping this document true

With every commit, ask whether the change made anything here wrong, and fix it in the same commit. Specifically:

Any token added or changed during implementation is written back into section 2 or 4 with its measured contrast ratio. Any new component is specified in section 5 before it is built twice. Any screen that departs from the reference file gets its departure and reason added to section 3, or gets corrected to match. Any new user-facing string follows section 7, and the AI-slop ban list in section 1 gets re-checked against current research before any significant new design work.

A design document that no longer matches the software is worse than no design document, because the next session will build against it and inherit the drift.
