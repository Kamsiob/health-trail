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
- **One recorded exception: the support button's outline.** D59, at the owner's direction. It is an **outline and never a fill**, so gold still means "the way in" everywhere it is filled, and its **label is `ink`**, so the half of this rule about text is kept exactly. It never shares a screen with the capture button. This is the only exception, it is written down rather than assumed, and a second one is a decision rather than a precedent.
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
| Mono L | JetBrains Mono | 28sp / 34 | 500, tracking 0, tabular | A number at display size, in the stat display 11.6 and nowhere else |

**Mono L carries no tracking, and that is deliberate rather than an oversight.** The 0.12em on the Mono style exists to raise letter distinction at 11sp, where the label is small and short. At 28sp the same tracking pulls the digits of a single number apart until it reads as several numbers, which is the opposite of what a stat display is for. Tabular figures do the alignment work instead, per 5.18.

**The two exemptions from the 13sp floor, and why they are the only two.** Section 3 item 4 sets a 13sp minimum. The nav label and the Mono metadata style sit below it, and both are deliberate.

Neither ever carries information on its own. A nav label is always paired with an icon and a content description. A Mono eyebrow, timestamp, or count is always directly above or beside the content it labels, and it labels rather than states: removing it would cost context, not meaning. Both are short, and Mono is uppercase and tracked, which raises its cap height and letter distinction well above what 11sp lowercase body text would give.

Both scale with dynamic type, so a person who has raised their system font size gets them larger, which is the case the floor exists to protect.

**The nav label is capped at 1.4, and it is the only capped type in the app.** At font scale 2.0 "Notebook" broke mid-word into two lines and collided with the capture button. A single word cannot wrap, so the only choices are to break it, clip it, or stop it growing, and a word broken across two lines is less legible than the same word slightly smaller. Four labels and a fixed clearance for the capture button share one row, which is a width budget nothing else in the app has. Above the cap the label holds and the icon, the position, and the content description carry it, which is the set of things a person navigates by after two weeks anyway. Found on the phone with the system font at maximum, which is why section 9 requires that pass rather than a reading of the code.

Nothing else may be added to this list. If a third candidate appears, that is a sign the layout is too dense, and the layout gets fixed rather than the floor lowered.

**Atkinson Hyperlegible is a deliberate choice, not an aesthetic one.** It was designed by the Braille Institute for maximum character distinction for low-vision readers. The audience for this app is stressed, frequently older, and often reading in bad light. Verify the current release name and license at build time and bundle it.

**Script coverage is a real problem to solve, not a detail.** Bricolage Grotesque and Atkinson Hyperlegible cover Latin. Arabic needs Noto Sans Arabic, bundled. In Arabic and Chinese, display text uses the relevant Noto face at bold weight rather than forcing a Latin display face that has no coverage. Do not ship a screen where one language renders in a different face than the rest of that language's screen.

**Chinese uses the system face and is not bundled. This is a rule, not a compromise.** Android ships Noto Sans CJK and renders it well, and it is the face a Chinese-reading person already sees in every other app on their phone. A bundled Noto Sans SC would add roughly ten megabytes per weight to reproduce something already present and already correct. Arabic is bundled because coverage there is genuinely inconsistent across devices; CJK is not, so the same reasoning gives the opposite answer.

**Never subset a bundled face, and never subset in order to make bundling affordable.** A subset covers the glyphs somebody thought to include. **This is a record-keeping app, and the thing most likely to fall outside a subset is a person's name or a facility's name**, which is exactly the content that must never render as a box. The cost of a missing glyph here is not a cosmetic defect: it is a record that cannot be read back. If a face is too large to bundle whole, that is an argument for using the system face, not for cutting the face down.

**Verify by rendering, on a device, per language.** Run the app under a per-app locale, `adb shell cmd locale set-app-locales <package> --locales zh-Hans`, and look at real strings rather than at a font file. **Include the mono eyebrow style**, which is the one most likely to fall back silently: a monospace family that has no CJK coverage will substitute a different face for those labels alone, and it reads as a slightly wrong screen rather than as an obvious error.

**Verified on the device on 2026-08-01, and the result is accepted.** Every Chinese glyph renders from the system face with no missing-glyph boxes anywhere, across headings, list rows, subtitles, counts, and the bottom navigation. **The mono eyebrow is served by the system CJK face rather than by JetBrains Mono**, which has no CJK coverage, so Android substitutes. That substitution is correct and is the intended outcome rather than a defect: the eyebrows read as eyebrows, quieter and smaller than the row titles, and the alternative would be bundling a monospace CJK face to style three words a screen. Latin digits inside an otherwise Chinese line, as in a count, keep the mono face, and the mixed line reads normally.

**Use `zh-Hans`, never a bare `zh`.** A bare `zh` carries no script, Hans and Hant are different writing systems rather than dialects, and Android's locale negotiation will not guess between them. Getting this wrong does not produce an error: it produces English. See `res/xml/locales_config.xml`.

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

**Section 11 is the other half of this section and it is not optional reading.** Section 5 specifies how each component is drawn. **Section 11 specifies which one to reach for**, which is what was missing for the app's first three weeks and is why every screen converged on 5.3. A component here that section 11 also names is governed by both: 5 for its geometry and states, 11 for when it is the right answer.

### 5.1 The mark

Two stacked rounded bars in `blaze`, **both the same width**, which is a painted trail blaze. Used at 22dp in the app bar, 44dp on the launch and About screens, and as the app icon on a `paper` field. Never enclosed in a circle, never given a gradient, never animated except the launch fade.

**Proportions:** each bar is 22 wide by 6 tall with capsule ends, and the gap between them is 5. Scale those proportions rather than choosing new values.

**Two corrections on 2026-07-31, both from looking at the built icon rather than at the specification.**

First, this section previously said the upper bar was slightly narrower than the lower. That contradicted `reference/screen-grid.html`, which draws both at `width:22px`, and the difference was not listed in section 3, where every deliberate override of the mockups is required to appear. It was an error in this document rather than an intended override, and it had already been built the wrong way once. **Equal widths, and that is not negotiable.**

Second, the reference draws the bars at 22 by 9 with a 3 gap, and built at icon scale that weight reads as a hamburger menu control. Thinned to 22 by 6 with a 5 gap and capsule rather than cut ends, which reads as brushed on rather than drawn. Listed in section 3 as item 9, because it is a genuine departure from the reference rather than a correction to it.

### 5.2 The trail, which is a system rather than a screen

The signature element, specified exactly:

- Route line: 2dp stroke, dashed 6dp on and 6dp off, `blaze` at 65% opacity, drawn behind the nodes.
- Node: 12dp circle with a 3dp ring in the current background color, so it reads as sitting on the line rather than beside it. Node color carries the entry type: `blaze` for a call, `blue` for a visit, `alert` for an incident, thread color when filtered to a thread.
- Care thread routes use the thread's own color at the same dash pattern. An ended thread drops to 35% opacity and keeps its color, so it reads as finished rather than deleted.
- In the milestone arc and chapter journey views the line is continuous rather than dashed, with larger 12dp ring nodes, because those are the person's actual path rather than a filter over entries.

**This was built once, on the timeline, and used nowhere else for a week.** That is the diagnosis behind everything in 5.2.1 through 5.2.5. Section 1 bans every cheap way to make a screen interesting, which was correct and which left nothing in their place, so every screen converged on the one pattern that survives the bans: a card with text in it. Twelve of those in a column is disciplined and it is also indistinguishable from every other utility app.

**The answer is not to relax the bans.** It is to use the vocabulary this app already owns. A trail map is native here rather than borrowed from a trend, and it is sitting on one screen doing nothing for the other twenty.

**The rule that makes it a system rather than a motif: a shape means the same thing everywhere it appears.** A person who learns the vocabulary on the trail can read a chapter list, a thread, or a search result without being taught again. A shape that means one thing here and another thing there is decoration wearing a system's clothes.

#### 5.2.1 Waypoints

**One node family, four states, and the state is the meaning.**

| State | Drawing | Means |
|---|---|---|
| Filled | 12dp disc, 3dp ring in the surface behind it | Something that happened. The default |
| Hollow | 12dp ring, 2dp stroke, no fill | Something upcoming, or expected and not yet here |
| Ringed | 12dp disc with a second 1.5dp ring 4dp outside it | A milestone. Rare by design: if everything is ringed nothing is |
| Gold | `blaze` filled, at 16dp | The capture button, and nothing else |

**Color carries the kind, the shape carries the state**, and the two are read separately. A hollow `alert` node is an incident that has not happened yet; a filled one is an incident that has. That is one drawing rule producing eight meanings, which is what makes it learnable.

**Every waypoint state survives grayscale**, because the shape does the work. Section 2.2's rule that every color carries a word alongside it is unchanged and applies here.

#### 5.2.2 Routes

**A thread's route identifies it everywhere the thread appears**, not only on the care threads screen. On the trail, on an entry that belongs to it, on the thread's own page, in a search result, and on Today.

**A route is a color and a dash pattern together, never a color alone.** The color is the thread's; the dash is assigned from a fixed list of four in the order threads are created: 6 on 6 off, 2 on 5 off, 10 on 4 off, and 1 on 4 off with round caps. Two threads that land on similar colors are still told apart in grayscale, by a colorblind reader, and on a phone in sunlight.

**A route is 2dp, and it never becomes a colored bar or a colored left border on a card**, which section 1 bans twice over and which already slipped into one draft.

**Beside a thread's name it appears as a swatch**, 28dp of the route itself. Not a dot. A dot is the color alone, which is exactly what 5.2.2 forbids, and it is what the care threads screen, the trail row, and the entry screen each carried independently until 2026-08-03. 28dp because four dashes of the widest pattern have to fit: two dashes is a dash pair, not a rhythm.

#### 5.2.3 Spines

**A chapter list, an incident thread, a milestone arc, and a medication's history across chapters are all the same shape:** a line with events on it. They looked like four unrelated lists, and they are one thing seen four times.

A spine is a vertical route with waypoints on it and content to its inline end. The gutter is 28dp with the line centered 9dp from the start edge, which is the geometry the trail was built and accepted at, and the node lands 40dp down the row so it sits on the row's first line of text rather than in its vertical middle. A long entry does not push its own waypoint away from the date it belongs to. The line is continuous rather than dashed when it is the person's actual path, which is a chapter journey or a milestone arc, and dashed when it is a filter over entries, which is a thread or a search result. That distinction is already in the last bullet of 5.2 and it now applies everywhere rather than in two named views.

**The 40dp is a ceiling, not a fixed offset, and it is clamped to half the row's own height.** It was measured against a trail card, which carries a date line above its title. A card with one line of text and no eyebrow is shorter than that, so a fixed offset put the node below the words it marks, near the card's bottom edge. Rows of both shapes sit next to each other on a prep sheet, where questions carrying a role label and questions without one are interleaved, and their waypoints visibly drifted apart down the spine. **A spine whose nodes do not line up with their rows reads as a rendering fault rather than as a trail**, which costs more than the alignment does. A short row centers; a tall one still anchors at its first line, which is the reason this is an offset and not simply a center.

**A spine mirrors, and the canvas has to be told to.** `Modifier.offset` is layout direction aware and the node used to be placed with one. Drawing it into the canvas to get the row's height traded that away silently, and in Arabic the line sat nineteen dp from the start edge instead of nine: the layout mirrored, the gutter moved to the correct side, the line was inside it, and it read as correct. **It was found by measuring the same screen in both directions rather than by looking at either one.** Fifty nine pixels from the start edge in English, eighty one in Arabic. The canvas mirrors `x` against its own width now.

**The node is drawn into the gutter canvas rather than placed as a composable**, because it needs the row's measured height and asking for that inside a row measured with `IntrinsicSize.Min` is not something Compose will do: `BoxWithConstraints` is a `SubcomposeLayout` and throws outright. The gutter is decorative and the row beside it always names the thing in words, so nothing is owed to the reader by the node not being in the semantics tree.

**A spine mirrors in right to left**, per 4.4, and the whole geometry flips rather than the line staying put.

#### 5.2.4 Markers, which are distance

**A trail map tells you how far apart things are, and a list does not.**

Between two adjacent entries more than fourteen days apart, the gap itself carries a mono line at the spine. One line, `ink3` text-safe, in the Mono style from 4.3.

**The word follows the reading direction of the list, and this matters.** The trail runs newest first, so reading downward is travelling backward, and the marker says "Three weeks earlier". A list that runs oldest first says "later". A marker whose direction disagrees with its list is worse than no marker, because the person is now doing arithmetic against the app instead of with it.

**Calendar units, never divided days.** A month is not thirty days and a year is not 365. Eleven months is months and twelve is a year, never "twelve months earlier", which reads as an app counting rather than reading a calendar. Held by `DistanceTest`, including both sides of every unit boundary and a gap spanning a daylight saving change.

**It costs one line and it turns a list into a story.** Two calls a week apart read as a week of calls; the same two rows with four months between them read as somebody who was left alone and then something happened. The list shows the same rows either way.

**It is never a judgment and never a warning.** "Three weeks later" is arithmetic on two dates the person recorded. Nothing anywhere says a gap was too long, and no gap is colored, per rule 2 and section 2.2.

**Fourteen days is the threshold** because below it the line appears constantly and stops being information. An unknown or coarse date never produces a marker at all, because the distance is not known and 10.9 forbids inventing precision.

#### 5.2.5 Texture, used once

**A very low opacity contour motif belongs in exactly one place: empty states.** Not behind content, not on cards, not as a hero. It gives an empty screen character without decorating a working one.

See 5.17 for the drawings themselves.

### 5.3 Cards

`card` surface, 20dp radius, 16dp padding, soft two-layer shadow in light and surface-step elevation in dark. No border. No colored edge. A card may carry a mono eyebrow label above its content, in `ink3` text-safe, 11sp, uppercase, tracked.

### 5.4 Buttons

Filled: `blue` fill, `on_blue` label, pill, 48dp minimum height, Label type.
Quiet: `card` surface with elevation, `ink` label, same geometry. **Use it where a real, common, structural action is not the point of the screen it sits on**, which is what "Add someone" on the care team is. As a filled button that was a full width blue bar and the loudest thing on a screen whose subject is the people above it, which inverts 10.8.
Text: no container, `blue` label, still 48dp of touch area.
Destructive: `alert` fill, white label, and only ever inside a confirmation flow, never as a resting state on a screen. It lives in `Confirm.kt` rather than with the other buttons, so it cannot be reached without also reaching the confirmation it belongs to.

**Removing something is reached by a long press, and that follows from the sentence above.** A Remove control resting on every row of every list is a destructive affordance sitting on the screen, which this section rules out, multiplied across eight sections. The long press is also what Android already means by "more to do with this row". It is declared as a gesture plus an explicit long click action rather than through `combinedClickable`, because that would also make the card answer a short press with nothing, which rule 16 calls broken, and because the explicit action is what puts removal in a reader user's list rather than leaving it a gesture they cannot discover.

**A tap opens the same form that created the row**, where one is offered. That is what makes a card answer a short press with something real rather than with nothing, which rule 16 requires, and correcting a typo then uses the screen the person already knows rather than a second near identical one. The heading is the only thing that changes between adding and correcting, so somebody fixing a number is never told they are adding a person.

**The confirmation says what happens, not "are you sure".** It stops appearing in the notebook, and nothing else the person wrote is touched. Both halves matter: somebody removing one row needs to know the rest is safe. It shows the thing back in the person's own words, so a wrong long press is caught before the tap that matters, and it never mentions tombstones, which are the schema's business per rule 20.
Support: the app's **only outlined button**. 2dp `blaze` outline, `ink` label, tile radius, same 48dp floor. One purpose and one destination, the canonical support link. It is the recorded exception in 2.2 and it must never read as a request: it sits after the sentence saying the app asks for nothing, and never before it. D59.

An action keeps the same word through its whole flow. The button that says Export produces a result that says Exported.

### 5.5 Bottom navigation and the capture button

**The four destinations carry icons**, drawn to the same rules as the twelve section icons: one 24 unit grid, a 1.7 stroke, round caps and joins, no fill, no more than three strokes. The slot had held a placeholder dot since the bar was built.

**Projects reuses its own section drawing**, because it is both a destination and a section, and two drawings for one thing is how two drawings start to drift. The other three are composed. **Today is a waypoint**, the ringed node the trail already uses, because today is where the person is standing on their own trail and this app owns a shape that means that; a sun or a clock would have been a stock icon saying nothing this app means. **Notebook is a bound book seen from the spine edge**, the object the screen is named after. **More is three dots**, because it is the one destination that is a drawer rather than a thing.

**Selection is carried three ways at once**, per 2.2: the icon's tint, the label's weight, and a 4dp dot beneath the icon. The dot is what the slot held before the icons existed, and it stays, so somebody who cannot separate the two blues still has two other signals. Checked at font scale 2.0, where the bar still holds one line per label and nothing meets the capture button.

Four destinations, always in this order: Today, Notebook, Projects, More. `card` container, 24dp radius, 8dp inset from the screen edges, elevated. Icon 20dp above an 11sp label. Active state is `blue_deep` on both icon and label, plus the label at weight 700, so color is not the only signal.

The capture button sits in the center of the navigation container, overlapping its top edge by 16dp: 56dp circle, `blaze` fill, dark plus glyph in `onBlaze`, present on every screen in all four tabs. The glyph is deliberately not white, for the contrast reason given in 2.4. It is the only way data enters the app and it never moves, never hides on scroll, and never changes color.

**The navigation leaves the center empty for it rather than sharing it.** Two destinations, a gap of the button plus 8dp of air on each side, then two destinations, with equal weights so each label centers in its own quarter. The four spread evenly across the whole width at first, which put the seam between Notebook and Projects exactly where the button sits and crowded both. **The gap is a column in the layout, not spacing**, so no label can grow into it: at the largest font scale the tabs narrow and the button keeps its clearance. D60, found by the owner looking at his phone.

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

**An empty state is the biggest character opportunity in this app and was the thinnest thing in it.** It is the screen a new person sees most, it has no content competing for attention, and it was a line of gray text. Every one now carries a line-art drawing from the set in 5.17.

**The shape of one, in order**, which is 10.8 applied to a screen with nothing on it:

1. The drawing, 96dp, centered, at the opacity 5.17 sets.
2. The invitation, in Display S. What this place is for, in the person's terms.
3. One line of Body M saying what turns up here and how it gets here.
4. The action, when there is one worth offering. Never a second one.

**No progress meter, no completion count, and no prompt to finish setting up**, per rule 13. An unfilled section reads as "not yet", never as a deficiency.

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

#### 5.11.1 The cap, and the full set behind it

**A chip set exists so the person can see every answer at once, and that promise breaks the moment there are twenty of them.** A year five notebook offers ten people, eight medications, and seven threads on one capture form, and what the person met was a wall of pills. **The pattern was right and it had no upper bound.**

**Five chips, then one more control.** The cap is five, and the sixth position is a `MoreChip` reading "Show all 9" that opens the full set with search. Chips wrap as before and nothing else about the group changes.

**The `MoreChip` is chip shaped and deliberately not a chip.** Its label is `blue`, which is what every action in this app is, and it is announced as a button rather than a radio button, because it is not one of the answers: it opens the rest rather than saying anything about what happened. Nothing else differs, because a second shape in the same wrapping row would read as a second kind of question.

**Which five, and why the reasoning lives in the query.** The set arrives in whatever order its query set, and the cap takes the head of that order without reordering it. For people that order is **most recently involved first**, computed from `entry_person`, so the five are whoever the person has been dealing with lately rather than the five added first. For medications it is the ones she is still on. **Putting the cap in the layer that has the data is what keeps it from being a guess dressed as a rule.**

**The chosen answer is always among the five**, even when it would fall outside, and it displaces the last of the head rather than being appended, so the row never grows past the cap. **A chip set that hides the answer the person already gave is lying about the state of the form.**

**The full set is a dense list, per 11.3**, in a sheet with a search field above it. The person opening it is scanning for one name they already have in mind, which is the exact case 11.3 exists for, and cards there would be the same wall with more space between its bricks. Selection is `blue_soft` plus the title at weight 700, which is this section's own language rather than a second one.

**Search narrows and never filters anything away permanently**, matching on plain contains-ignoring-case and never fuzzily: somebody typing three letters of a nurse's name and being shown a different nurse is worse than being shown nothing, because this list writes a link into the record. Nothing matching says so in a sentence.

**Below the cap nothing appears.** A set of four chips shows four chips and no control, because "show all 4" is a control that does nothing, which 5.14 and D42 both rule out.

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

#### 5.12.1 The set, and the rule that makes a tile grid work

**Twenty three drawings: thirteen sections, four destinations, and six capture kinds.** Two of the six are not drawings of their own and must never become drawings of their own: a captured question uses the Ask next time icon and a captured document uses the Documents icon, because those are the sections they land in and **a shape means the same thing everywhere it appears**, per 5.2.

**The rule the set is held to, which is stricter than "one grid, one stroke":** no two drawings share a silhouette. In a list, an icon is a decoration beside a word somebody is going to read anyway. **In a tile grid the icon is how the person finds the tile**, and a grid of shapes that need reading to be told apart is only a shorter list, which is the one thing 11.2 says a tile grid must not be.

**Four section drawings were changed on 2026-08-03 for exactly that reason**, and none of them looked wrong on its own:

| Section | Was | Is | Why |
|---|---|---|---|
| Medications | A blister pack, a tall rounded rectangle with a band | A capsule, split across the middle | It was the fourth rectangle in a set of thirteen. The capsule is the only fully rounded shape |
| Money | A receipt torn along the bottom | A note seen wide, with a mark in it | A tall narrow shape with a notched bottom is a bookmark, which is Chapters. It is now the only short wide shape |
| The trail | A curve, a bar, and three nodes stacked at the end edge | One winding route with three waypoints on it | At tile size it read as Care threads: dots on one side with lines running off them. A route carries its own nodes everywhere else in the app |
| Projects | A folder | A marker flag on a post | A folder is a stock drawing meaning "some files", and it shared a silhouette with the calendar and the clipboard. A project is a sequence with a destination, per 11.12, and a trail already has a shape for the end of a route |

**The trail is allowed a fourth stroke**, against the three stroke rule the rest of the set keeps. It is the app's signature rather than a section marker, and a route with no waypoints on it is a squiggle.

**Two of the four capture drawings are deliberately conventional and two are deliberately not.** A call is a handset and a visit is a pin, because those are the symbols every person already reads and inventing a novel shape there would cost the person a beat for nothing. **An incident is a route with a break in it and a marker standing in the gap**, which is this app's own vocabulary rather than the warning triangle every other app reaches for: a triangle would be the interface sounding an alarm about something the person calmly wrote down, and rule 2 rules that out. **A measurement is a dial with a needle and no markings of any kind on its face**, per 5.8, because a face with zones drawn on it would be the app judging a value.

**The whole set is rendered on one sheet by `tools/icons/sheet.py`**, at 44dp, 32dp, and 20dp, because an icon is judged at the size it is used rather than at the size it is drawn. **The tool reads the paths out of `SectionIcon.kt` rather than holding a copy**, so a sheet that agrees with itself cannot disagree with the app, which is D66 and D68 applied to a preview. **Both collisions above were invisible in the source and obvious on the sheet.** Run it before adding or changing a drawing, and look at the whole set rather than the one being changed.

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

### 5.17 The empty state drawings

**One set, drawn once, so they are recognizably siblings.** A path, a waypoint, a contour. Line art in `ink` at 12% opacity, on the 24 unit grid with the 1.7 stroke from 5.12, round caps and joins, no fill.

**A drawing is the section's own icon, large, standing on a trail map ground.** The ground is the sibling half and is identical everywhere: two contour lines and a route running between them with one waypoint on it. The icon is the identifying half, and it is the same path already drawn in the table of contents at 20dp, scaled up rather than redrawn.

**This is why there are thirteen of them and none of them was invented.** A set of thirteen freshly drawn illustrations would drift in weight and character no matter how carefully it was made, and the app already owns thirteen drawings that do not. Reusing them also means the empty screen is teaching the icon a person will navigate by for the next two years, at the moment they have nothing else to look at. Composing inside an existing idiom rather than inventing one, per 10.2.

**Banned here specifically**, restating section 1 because this is where illustration usually goes wrong: no 3D, no blobs, no plastic, no stock imagery, no mascot, no character, no scene with a person in it, and no color. A drawing that could carry a brand's mascot is not this set.

**They are decorative in the sense 2.3 defines**: remove every one and nothing becomes unreadable, because the words carry the screen alone. So they are exempt from the 3:1 contrast ratio, and they carry no content description, because a reader announcing "line drawing of a path" on every empty screen is noise rather than access. They are marked as decorative for the reader and skipped.

**The same drawing means the same thing everywhere**, exactly as waypoints do. A section's empty state uses the drawing for that section, never a generic one, so the empty screen is already teaching where you are.

### 5.18 The disclosure

**The rest of a form, behind one control nobody is required to touch.**

**Progressive disclosure is part of hierarchy rather than a separate feature**, per 10.8, and everything visible at once is the most common structural tell on the ban list in section 1. The capture form put roughly twenty three controls in front of somebody standing in a corridor, which is the opposite of forgiving.

**Geometry.** A text action reading "Add more", per 5.4, with one line of Body S beneath it saying that none of it is needed. It expands with the standard spring from section 6 taken through `LocalMotion`, so reduced motion turns it into an immediate state change rather than nothing.

**It opens and stays open.** There is no close control, because a person who opened it wanted what is inside and taking it away again would be the form arguing with them. Leaving the screen resets it, which is right: the next capture starts from the short form. It survives a rotation and a theme change, so nobody part way through filling it in gets folded back up.

**It never carries a count and never says how much is left.** "Add more" is an offer. "3 more fields" would be a measure of how incomplete the entry is, which rule 13 rules out.

**Nothing inside is ever required, and the aside says so once.** A disclosure hiding something the person has to fill in is not disclosure, it is a trap.

**What goes behind it** is what the app can work out or live without. On capture that is the care thread, because an entry with no thread lands in the Unfiled tray where the app suggests a home and asks for one tap, and the medication a question is about, because most questions are about nothing in particular.

**What must never go behind it** is anything telling the person what will happen to what they wrote. The capture form's line about the Unfiled tray stays outside and sits directly under the control that would let them change it, because for somebody who never opens the disclosure it is the only thing saying where their entry is going.

**A disclosure with nothing in it is not shown at all.** An empty room to walk into is the same defect as an empty section.

### 5.19 Numerals

**Every count, date, dose, amount, and duration is set in tabular figures.**

Columns of numbers that align can be scanned; numbers that jitter cannot, and this app is full of numbers sitting in lists. It costs one font feature.

`tnum` on the Mono style, which already carries counts, timestamps, and metadata per 4.3. Where a number appears inside body text, as in a dose or an amount, the number takes the Mono style and the words around it do not, which is the same treatment the trail already gives a date.

**This is legibility rather than style**, so it applies retroactively to every screen already built, per rule 14.

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

**The capture sheet, rebuilt as a tile grid on 2026-08-03.** Six choices as a vertical list of `sand` rows was six things to read, and 11.1 says a fixed set of destinations chosen by position is a tile grid rather than a list. It is now two by three, each tile carrying its capture drawing from 5.12.1 above its label. **One column above font scale 1.3**, per 11.2's table, and the grid gives way rather than the type.

**The tiles are `sand` rather than `card`, which is the one departure from 11.2**, because the sheet itself is `card` and a card tile on it would be a shape with no edges. The recessed surface is what 2.1 sets aside for exactly this, and it is what the six rows already used. **The icon inside carries no fill**, per 5.12's standing weight, since a `sand` icon tile inside a `sand` tile is invisible, and its drawing is `ink` rather than `ink2` because here the icon is the content rather than a marker beside a row. **There is no count slot**, because a capture kind is a thing to do rather than a place with things in it, and an empty count line would be an empty area.

**The sheet's title had no top padding and the 28dp corners cut into it.** "What happened" sat on the content's first pixel with its ascenders running into the sheet edge, and it read as a sheet that had failed to finish opening. It is the first thing anybody sees after tapping the one control the whole app is for. Found by opening it and looking, not in review.

**The capture form, reordered and capped on 2026-08-03.** Three changes, all from using it against a year five notebook.

**What happened comes first, and it used to come last.** The person taps this having just put a phone down, and the thing in their head is the sentence they came to write. It sat below the who field, the care team chips, the date chips and the thread chips: twenty three controls between somebody and one sentence. Rule 15 says the thing that matters most gets the most weight and the best position, and this form had it backwards in exactly the way the entry screen did. **The fastest path through the screen is now type and save.**

**Every long chip set is capped at five with the full set behind one control**, per 5.11.1. The people offered are the five most recently involved, computed from `entry_person`, rather than the five added first.

**The care thread and the medication question are behind a disclosure**, per 5.18. What stays outside it is the line saying an entry with no thread goes to the Unfiled tray, because that is the only thing telling somebody who never opens it where their entry is going.

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

**The steps are controls, and only the ones still worth taking appear.** They were three fixed sentences that led nowhere, written when the screens they named did not exist. The screens exist, so a step that tells somebody to fill in the emergency card and leaves them to go and find it is a dead end wearing a suggestion. Each step now opens what it names, and disappears once it has been taken. **A notebook with nothing left to suggest shows no coaching at all**, because an empty list is a finished state and inventing a fourth suggestion to fill the space would be the app keeping score, which rule 13 forbids.

**The numbering is allowed here**, and section 1 bans numbered markers only where the content is not genuinely a sequence. Filling in the emergency card first is the entire point of the list.

**Whether the emergency card counts as filled in is decided the same way on both screens.** The coaching asked the row count, which only knows whether a card row exists, while the card itself counts its contacts and the medications marked for it. The result was Today advising somebody to fill in a card that already listed a medication. Two screens answering the same question differently is the app being wrong about itself, and this is the second time this seam produced a contradiction.

**The digest leads, and it says only what the change log says.** Each section that gained something is a row drawn like the notebook's own, because it is the same idea: a place and how much is in it. Somebody reading "the trail, 2 new" is already asking which two, so the row opens the trail. **Ordered by where things live, never by how many**, since ranking by volume would move the sections between visits and the notebook's one promise is that the places never move. Corrections and removals are stated quietly, as totals with no destination: they are usually somebody tidying up after themselves, and giving that the weight of new records would turn the screen into a report card on how tidy they are being.

**A quiet week says nothing rather than saying nothing happened.** A heading over "nothing changed" is a heading over nothing, and this screen is read at a glance. The same is true of a first launch, which has no previous visit to be "since" and does not summarize a notebook's whole history as though it happened this week.

**The screen led with an apology for eleven days.** While the engine did not exist, the most prominent thing on the app's front door was a note that the summary was still being built. That was correct under D44 and it was still the front door leading with what the app could not do, which is the argument for building the engine rather than for wording the note better.

`TodayScreenTest` asserts the empty state never scolds and never keeps score, which is rule 13 checked rather than remembered.

Composed from Display L, Display S, Body L, Body M, the Mono style, and cards 5.3. Tracked on issue #78.

**Exporting the notebook.** Undesigned, composed, and logged per section 10. Issue #126.

**The sentence the format requires appears before the person commits**, in the words `contract/export-format.md` section 4 requires: if the passphrase is lost the file cannot be recovered, there is no server, no recovery code, and no backdoor.

**The passphrase is typed twice**, which nothing else in this app asks of anybody. A typo here is not a typo, it is a file that looks like a backup and can never be opened, and that is the one failure worth an extra field.

**It is concealed by default with one control to reveal it.** It rendered in the clear until 2026-08-02, because a password keyboard selects a keyboard and masks nothing. Concealing it outright was rejected: typing a passphrase blind, twice, and being told only that the two do not match is a trap for anybody tired, and this screen is used by people who are.

**An unencrypted export is offered plainly rather than hidden**, with a warning rather than a scolding. It is their data and wanting to read it is reasonable.

**The result replaces the form.** "Saved" sat in body text below two still live buttons, which gave the one thing that had just happened the least weight on the screen and invited a second file nobody asked for. It now says to open the file once, while the person is still here, rather than trusting it unopened.

**Restoring from a file.** Undesigned, composed, and logged per section 10. Issue #127.

**Five states, and the two that only appear when something has gone wrong are the ones that were designed hardest.** Choosing a file reads the manifest and nothing else. A wrong passphrase says the passphrase is wrong or the file was altered and there is no way to tell which from here, because GCM genuinely cannot distinguish them, and it leaves the field in place to try again, since the common case is a typo and making somebody start over punishes the likeliest mistake.

**What is in the file is shown before anything is applied**, and the sentence saying a restore replaces everything sits above the button rather than after it.

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

### Appearance, and More around it

Not in `reference/screen-grid.html`. Composed under section 10 and logged here at the moment it was built, per rule 12. Tracked on issue #88, review on #89.

**Three choices, and following the phone is the default.** Overriding what somebody has already told their device is presumptuous. The setting exists for the case where they want this one app to differ, which is a real case rather than a hypothetical: reading long text in a dark room, or in a bright corridor at three in the morning, is the situation this app is for.

**It applies immediately, and that is the requirement rather than a nicety.** The choice is held above the theme in `MainActivity`, because state read inside the theme cannot change the theme reading it. A setting that needs a restart reads as one that did not take.

**Following the phone means following it while the app is open**, not at launch. `isSystemInDarkTheme` is read inside composition rather than resolved once, so a phone that flips at sunset takes the app with it.

**Selection is state, not a mark.** The row carries `selected` in its semantics as well as the dot, because a reader user who cannot see the dot would otherwise meet three identical rows. The marker is the bottom navigation's own selected dot rather than a new checkmark path: one drawing vocabulary, per section 10.2.

**More says what else is coming, and that is not filler.** Appearance is currently the only thing in More, and three cards above two thirds of an empty screen reads as unfinished, which rule 14 forbids. The honest fix is not a fourth invented setting. It is to name what this destination will hold, which is D44 applied to a whole screen rather than to one control. It carries `ShellTags.NOT_BUILT` so it cannot survive to release.

**More is not a list yet, deliberately.** A menu of one item is a tap paid for nothing. It becomes a list when there is a second thing in it.

**One consequence worth recording outside this file.** `tools/screenshot.sh` used to read the theme from the device, per D31. With this setting the device is no longer the answer, since the app can be dark on a light phone. The script now reads the app's own stored choice first and consults the device only when that choice is to follow it. It also means both theme sets can be captured without touching anybody's system settings, which is what this screen was partly built to remove.

Composed from Display L, Body M, the Mono eyebrow with its hairline 5.13, cards 5.3, the press treatment 5.14, and the navigation's selected dot. Nothing new was introduced.

**The trail.** The record read back, and the app's signature element.

**It is drawn in section 5.2 rather than in the reference file, so it is specified without being mocked up.** That specification was followed exactly: the dashed route at 2dp, 6dp on and 6dp off, `blaze` at 65%, behind a 12dp node ringed 3dp in the current background so the node sits on the line rather than beside it. Node color carries the entry type, gold for a call, blue for a visit, alert for an incident.

**Everything else takes the quiet non-text ink rather than borrowing one of the three.** Section 5.2 names three kinds and the app has nine. A measurement wearing the incident color would be the app saying something about it that is not true, which is rule 2 reached through a color rather than through a word.

**It was built wrong first, and that is worth recording.** The first version was a plain list of cards. It compiled, ran, showed the right data in the right order, and was exactly the "functionally correct and visually plain" that section 10.5 says is not done. Nothing about the data changed in the rebuild. The screen went from a list to the thing the whole app is named after.

**Months head their own runs** through the 5.13 group header. A month heading carries no hedge, unlike every other coarse date in the app: it is naming the run of entries beneath it rather than claiming something happened across the whole month, and each entry still states its own date at its own precision.

**An unknown date gathers at the end under its own heading.** Not at zero, not at today. Both would place an entry on the timeline where the person never put it.

**The date is the one tappable thing on a row**, per rule 17, and it opens the same picker every other date uses. The rest of the row does not respond, which is the honest way to keep rule 16: only the thing that responds is made to look touchable.

**The route mirrors for free**, because it is drawn in the start edge rather than the left one, which is what section 8's right to left requirement asks of a timeline.

**It draws itself in once per screen entry**, 400ms with nodes staggered 30ms behind, through the section 6 tokens rather than inline, so reduced motion reaches it. `Motion.trailDraw` and `trailNodeStaggerMillis` were written for this screen long before it existed and had never been called.

Composed from Display S, Body M, Body S, the Mono metadata style, cards 5.3, the group header 5.13, the press treatment 5.14, and the trail tokens of 5.2. Tracked on issue #111.

**The care team, and adding somebody to it.**

**The number is the action, not a line of text.** The value of writing somebody down is that reaching them is one tap later rather than a search through a phone, an email, and a discharge folder. So the row carries a call action rather than a number to read out and dial by hand. It opens the dialer through `ACTION_DIAL` rather than placing the call, so no permission is asked for and the person presses the green button themselves.

**Every field is optional, including the name.** Somebody in a corridor with a number on a scrap of paper should be able to keep it. A row therefore takes its heading from whatever was actually given, name first, then the number, then the role, and **never invents a placeholder**, which rule 11 forbids and which would also be the app characterizing somebody it knows nothing about. When the number is the heading, the action says "Call" rather than repeating it two lines below itself.

**Nothing at all is a stray tap rather than a partial answer.** Any one field alone writes a real row. All three empty writes nothing and says nothing about it, because there is nothing to keep and an error message would be the app scolding somebody for a mis-tap.

**"Add someone" is a quiet button, and that is what introduced one.** Section 5.4 has specified a quiet variant since the beginning and nothing had ever used it. As a filled button this was a full width blue bar and the loudest thing on a screen whose subject is the people above it, which inverts 10.8: the accent belongs on reaching somebody, not on the way to add one. Checked on the device before and after, which is the only reason it was caught.

Composed from Display L, Display S, Body M, Body S, the Mono eyebrow, cards 5.3, the text field 5.9, the filled and quiet buttons 5.4, and one text action. Tracked on issue #111.

**The emergency card, and filling it in.**

**It is designed to be handed to a paramedic**, per `MASTER_SPEC.md` section 4.6, and that decides the whole screen. Somebody reading it is standing up, holding another person's phone, under time pressure, and wrote none of it.

**So the value carries the weight and the label recedes**, which inverts the usual order on purpose. On a form the label is what you are looking for. Here you already know what you are looking for and you need the answer, so the answer is Display S and the label is the quiet mono eyebrow above it.

**Only what has been filled in appears.** An empty field is not shown as an empty field, and a group header appears only when its group has something under it. Three lines that are all true beat nine where six say nothing, and under time pressure the blanks are noise somebody has to read past. This is rule 13 doing real work rather than being quoted: the card is complete at any size.

**The whole card is `alert` toned**, which section 2.2 gives to this one section and nothing else. That produced two color pairings no other screen makes, blue action text and `ink` values on `alert_soft`, and both are now measured by `check_contrast.py` rather than assumed. Neither existed before this card and nothing would have caught them.

**Who to call comes first, above everything else.** Somebody holding this phone in an emergency needs a number before they need a blood type: the number reaches a person who knows the rest. The paperwork below is what they read once the call is made.

**Contacts are chosen from the care team, never typed a second time.** Everybody there already has a name and a number in this notebook, and asking again would be the interface making the person do the app's filing, which rule 20 forbids. One tap puts somebody on the card, one tap takes them off, and taking them off the card never takes them off the care team.

**The card keeps its own copy of the name and number**, with the link recorded alongside it. A card that goes blank because somebody archived a care team row is a card that fails at the worst possible moment.

**The resuscitation line is the sharpest content rule in the app.** It is stored and shown as **what the signed paperwork says**, in the words on the form, beside where the original is kept. The app does not summarize it, does not shorten it to an abbreviation, and never says what it means. It is not a choice between options and it must never become one: that would be the app interpreting a legal document, which rule 2 forbids and which cannot be done correctly across fifty states. **A card that is wrong about this is worse than no card**, and the only safe thing the app can do is repeat the sentence and say where the paper is.

**Blood type asks "only if you know it for certain."** A guessed blood type on a card handed to a paramedic is worse than a blank one, and the honest way to get that is to say so rather than to reject input, which would be validation the rest of the app does not do.

**Every field can run long and none is held to a single line.** An allergy is usually a sentence and the resuscitation line is a quotation.

**Filling it in retires Today's coaching**, because the coaching existed to get this done. Confirmed on the device: the coached list is gone the moment the card has anything on it.

**What they take is assembled from the medications, never copied onto the card.** A medication carries the flag saying it belongs here, so the card is built from the ones that say so and neither has to be kept in step with the other. **A medication that is stopped drops off the card by itself**, which is the behavior somebody would expect and the one that is dangerous to get wrong. Rule 18, and the flag living on the medication is what makes the link work in both directions.

**On the card the name is the value and the dose is the detail under it**, which is the reverse of the medications screen where the two are read together. Same principle both times: the thing being looked for carries the weight.

Composed from Display L, Display S, Body M, the Mono eyebrow, cards 5.3 in `alert_soft`, the group header 5.13, the text field 5.9, and the filled, quiet, and text buttons of 5.4. Tracked on issue #113.

**Medications.**

**Record only, and the screen says so in its own subtitle.** `MASTER_SPEC.md` section 4.3 requires it to state plainly that the app does not remind or alert. Somebody arriving at a screen called Medications reasonably expects reminders, and the honest place to correct that is the moment they arrive rather than after they have relied on it.

**A dose is never parsed into a number and a unit.** The schema says so and the screen shows the sentence the person was told, in the words they were told it in. The field says out loud that the app never reads it as a number, which is permission rather than a disclaimer: "half a white one, twice a day, with food" is what people are actually told, and without that line they try to convert it into something that looks official.

**Nothing is checked against anything.** No drug database, no interaction check, no spelling correction. Each would be the app knowing something about medicine, which rule 2 forbids outright.

**A stopped medication stays**, moves below the current ones, and says it stopped. It is quieter but never struck through: it is still true, it is simply no longer current. "She was on this until March" is the answer to a question somebody will eventually be asked.

Composed from Display L, Display S, Body L, Body M, Body S, the Mono eyebrow, cards 5.3, the text field 5.9, choice chips 5.11, and the quiet and filled buttons of 5.4. Tracked on issue #114.

**Ask next time.**

**This section counted zero for as long as it existed.** Capture wrote a trail entry and nothing else, so a question appeared in the trail while the section that exists to hold questions said "Nothing yet" forever. **A section that counts nothing while the thing it counts is actively being captured is the app being wrong about itself**, which is worse than the section not existing. Both rows are written in one transaction now.

**Who it is for is its own column, never folded into the question.** The schema keeps `role_label` so a question waiting on the wound nurse does not turn up on the prep sheet for a billing meeting, and the screen reads it as the eyebrow above the question. Joining the two into one string was the first attempt and threw that away.

**Asked questions stay**, in their own group, carrying the date. "We asked in March and were told it would be reviewed" is exactly what somebody needs six months later, and clearing it the moment it is asked discards the half that matters.

**Marking one asked is one tap and never demands an answer.** The answer usually does not arrive in the same conversation, and requiring one would make the person either invent something or leave the question open when it is not.

Composed from Display L, Body L, Body M, the Mono eyebrow, cards 5.3, the group header 5.13, and one text action. Tracked on issue #115.

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

**Section 11 is the library this rule assumes, and until 2026-08-03 it did not exist.** For a week "compose, do not design" meant composing from a card, because that was the only piece with a stated purpose, and the result was twenty screens made of one shape. **Read section 11 before composing anything**, and pick the component from the shape of the content per 11.1 rather than from what the last screen happened to use.

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

## 11. The component library, and the layout patterns built from it

**The diagnosis, written down once so it never has to be found again.** Section 1 bans every cheap way to make a screen look interesting, and it was right to. Section 10.2 says compose rather than design, and it was right to. Together they left exactly one thing standing: **a full width rounded card containing text.** Every screen in this app was assembled from that one piece. Grouping twelve of them under headers is organization, not hierarchy, and a notebook, a search result, a bill, a person, and a project all arriving as the same rectangle is why the app reads as uninspired to anybody holding it.

**Composing from a library of one produces exactly this.** The instruction was right and the library was never built. This section is the library.

**The bans stay, all of them.** Nothing below relaxes section 1, and every component here is monochrome, unornamented, and made from shapes the app already owns. But understand the other failure mode as clearly as the first: **avoiding generated-looking design does not mean avoiding design.** Plain is not a virtue here. Crude is not authenticity. The target is a considered, warm, confident interface with real visual variety, not a stripped one.

**A component is not defined until this section says when to use it and when not to.** That is the half section 5 kept leaving out, and it is the direct cause of the card spreading everywhere: nothing ever said what a card was for, so it became what everything was made of.

### 11.1 The choosing rule

**Pick the component from the shape of the content, never from the screen it lands on.** One question decides it: what is the person doing with this thing?

| What the person is doing | Component | Why that one |
|---|---|---|
| Choosing among a known, fixed set of places | Tile, 11.2 | Recognized by icon and position, not read |
| Scanning a long list for one item they already have in mind | Dense row, 11.3 | Twice the rows per screen, and a fixed shape to scan down |
| Reading something with real substance | Card, 11.4 | Three or more lines, read rather than scanned |
| Taking in the single thing they opened the screen for | Hero block, 11.5 | One per screen at most, and often none |
| Reading a number that matters on its own | Stat display, 11.6 | Tabular mono at display size, never interpreted |
| Finding a photograph, a bill, or a document | Thumbnail, 11.7 | Their own paper beats its filename every time |
| Finding a person | Avatar, 11.8 | Initials are scannable, twelve names in a column are a wall |
| Switching between two views of the same content | Segmented control, 11.9 | Two or three, and it never navigates |
| Following something that happened in order | Spine, 5.2.3 and 11.11 | A line with events on it, which the app already owns |
| Following a link to something with a face | Inline preview, 11.10 | A link that shows what it points at costs one tap less |

**When two components would both work, take the quieter one.** A card where a dense row would do is the specific defect this section exists to correct, and it is far more common than the reverse.

**Every component below responds to a press**, per 5.14, using the one treatment. **Every one has a resting, pressed, and focused state**, and none of them invents a second selection language: selection is surface plus weight plus a ring, as 5.11 already sets it.

### 11.2 The tile

**Roughly square, in a grid: an icon tile above a name above a count.** This is the single largest change available to this app. Twelve notebook sections as a grid is a third of the scroll length, it is scannable by shape and position rather than by reading, and it immediately stops looking like a settings screen.

**Two sizes, and only two.**

| | Standard | Compact |
|---|---|---|
| Grid | Two columns | Three columns |
| Width at 393dp | 170dp | 112dp |
| Icon tile, per 5.12 | 40dp, drawing 24dp | 32dp, drawing 18dp |
| Name | Display S, up to two lines | Label, up to three lines |
| Count | Mono | Mono |
| Padding | 16dp | 12dp |
| Minimum height | 132dp | 108dp |

**Both use the card radius, 20dp, and the `card` surface.** A tile is a card in a different proportion, not a new surface, and a third radius would be a token invented for nothing.

**Content is start aligned, not centered.** Centered icons over centered labels is the launcher grid every phone already has, and it reads as an app drawer rather than as a table of contents. Start alignment also survives a three line name without the block drifting, and it mirrors correctly in Arabic without further thought.

**Order inside the tile, top to bottom:** the icon tile, 12dp, the name, 4dp, the count. The count sits under the name rather than beside it so that the tile has one column of content and one reading direction.

**Weight is carried by the icon tile's fill, exactly as D33 and 5.12 already set it**, and never by the tile's own surface, its size, or a hue. A section the situation template puts forward gets a `sand` filled icon tile at standard size. A standing section gets an unfilled tile at standard size. A folded section gets an unfilled tile at compact size in the quiet ink. **Three weights, two sizes, one fill rule, and no new vocabulary.**

**Columns drop as the font grows, and the tile never shrinks its text.**

| Font scale | Standard grid | Compact grid |
|---|---|---|
| Up to 1.3 | Two columns | Three columns |
| 1.3 to 1.8 | Two columns | Two columns |
| Above 1.8 | One column | One column |

A one column tile grid is a column of short wide tiles, which is a legitimate layout and not a fallback. **A tile whose name wraps to five lines is a card wearing a tile's clothes**, and shrinking the text to avoid that would break the 13sp floor in section 3 item 4. So the grid gives way, not the type.

**States.**

| State | Treatment |
|---|---|
| Resting | `card` surface, fill per its weight |
| Pressed | The tile's surface steps 8% toward `ink` over 120ms, per 5.14 |
| Focused | 2dp `blue` ring at the card radius, per 5.14 |
| Nothing in it | The count reads as words. "Nothing yet", never `0` |
| Longest language | The tile grows and its whole row grows with it, so a row of tiles is always one height |

**When not to use a tile.** Never for an action, which is a button. Never for a list that grows, because a grid of unknown length is a wall of squares nobody can hold in their head. Never where the person is looking for one specific named item, which is a dense row. **Never more than twelve in one grid**, and if a screen has more than twelve destinations the screen has a structure problem that a grid will only hide.

### 11.3 The dense row

**One or two lines, 52dp to 56dp, no card, separated by a hairline rather than by a shadow and a gap.** For a long list the person is scanning for something they already have in mind.

**Not everything deserves to be a card, and making everything a card is the reason nothing stands out.** A card row plus its 12dp gap costs 88dp. A dense row costs 52dp and carries the same two lines. On a five year notebook that is the difference between a list you scroll and a list you scan.

**Geometry.** Full bleed to the screen's own 20dp padding. 52dp for one line of content, 56dp for two, both clearing the 48dp touch floor without invisible padding. 12dp between the leading element and the text. The hairline is 1dp of `ink3` non-text at 40%, **inset to the start of the text** so it runs under the words rather than cutting across the leading element, and there is no hairline after the last row of a group.

**The shape is fixed everywhere in the app, and that is the whole point:** the leading element, then what it is, then when, then state. Same slots, same order, same positions, on every screen. A person who learns to read one dense row can read every list in the app.

- **Leading**, optional, exactly one of: an avatar 11.8, a thumbnail 11.7, a waypoint 5.2.1, a thread swatch 5.2.2, or nothing. Never an icon tile, which belongs to tiles and to the table of contents.
- **Line one:** what it is, Body L in `ink`. The thing's own words where it has them, per the entry heading rule.
- **Line two**, optional: Body M in `ink2`, one line, and it says something true about this row rather than repeating its category.
- **Trailing:** at most one of a Mono date, a Mono count, or a pill 5.6, and then the chevron where the row opens something. Two trailing elements plus a chevron is where a dense list starts to look busy, and there is always one that can be moved to line two.

**Groups use the group header 5.13**, which is the existing pattern and needs nothing new.

**States.** Pressed steps the row's own surface 8% toward `ink` across the full bleed width, per 5.14. Focused draws the 2dp `blue` ring at the tile radius inset to the screen padding, because a full bleed ring against the screen edge reads as a border on the window. Long press removes, per 5.4, and the row exposes the explicit long click action so a reader user can reach it.

**When not to use a dense row.** When the person will read rather than scan. When the content genuinely needs three or more lines. **And when there are fewer than five of them**, because four dense rows on an otherwise empty screen read as the top of a longer list that failed to load, where four cards read as four things.

### 11.4 The card, kept and demoted

Section 5.3 keeps its geometry unchanged. What changes is what it is for, which was never written down.

**A card is for content with real substance that the person reads rather than scans:** three or more lines, or mixed content, or one thing that is the subject of its own region. An entry with a paragraph in it, a bill with its state and its amount and what it was for, a standing instruction with its tag and its wording.

**A card is not for:** a list of destinations, which is a tile grid. A list scanned for one item, which is dense rows. Anything one line long. **A card whose entire content is one line and a chevron is a dense row that was given a shadow**, and that describes most of the cards in this app as of 2026-08-03.

**No more than six cards in one scroll region.** Past six, a card is no longer emphasis, it is the background, which is exactly how the app arrived here. Six is a real ceiling and a screen that wants eight is a screen whose rows are not all cards.

### 11.5 The hero block

**Exactly one per screen, at most, and no hero at all is a valid screen.** It is the single thing the person came for, given the most weight through size, position, and space, per 10.8 step 2.

**It is not a card and it must not be one.** It sits directly on `paper`, with no surface, no shadow, and no border. That is what makes it read as the top of the page rather than as the first item in a list, and it is hierarchy achieved without decoration, which is what rule 15 asks for.

**Structure, in order:** a mono eyebrow saying what this is, per 5.13's label style without its rule; then the thing itself in Display M, or Display L where it is very short or is a number; then at most one line of Body M supporting it; then at most one action, in the quiet button style from 5.4 or as a text action.

**Space is the component.** 24dp above and 32dp below, and the 32dp is the part that gets sacrificed first when a screen is built to be merely correct. Without it the hero is just a larger row.

**An optional leading element**, one of a waypoint, an avatar, or a thumbnail at 56dp, aligned to the first line of the heading rather than centered on the block.

**A hero is the answer to 10.8 step 1, rendered.** Decide what matters most on the screen, name it out loud, and the hero is that thing. **If you cannot name it, the screen does not have one**, and inventing a hero to fill the slot is worse than leaving it out: a banner carrying nothing is the decorative header this document bans.

**When not to use one.** On a screen that is a list of genuine peers, such as search results or the trail, where promoting one item would be the app making a judgment. Never two on one screen. **Never as a count of the person's own diligence**, per rule 13, which rules out "4 of 7 steps done" as a hero forever.

### 11.6 The stat display

**A number at display size in tabular mono, with a small mono label beneath it.** For a count that matters on its own.

**Geometry.** The number in Mono L, 28sp, tabular, from 4.3. The label under it in Mono, 11sp, uppercase, tracked, `ink3` text-safe. 4dp between them. **The label goes below the number, not above**, because the number is the point and the label is what it is a number of.

**Never with an arrow, never colored by value, never interpreted.** Section 1 bans the small colored arrow beside a number twice over, and rule 2 bans the interpretation underneath it. No delta, no comparison to last month, no target, no sparkline beside it, no color that changes with the value. **The number is a fact the person recorded, and the app counts it and stops.**

**At most two on a screen, side by side.** Three across is the three-cards-in-a-row shape from section 1 wearing numbers.

**Zero.** Where the number counts things the person made, zero renders as words, per the notebook's own rule, and the stat display is not used at all. Where it counts something that happened, such as the times a standing instruction was not followed, **zero says nothing and the display is absent**, because a count of nothing is not a finding and printing it turns every instruction into a scoreboard with most of the scores at zero.

**Where a count needs a sentence saying what it is not**, per `MASTER_SPEC.md` 4.11, that sentence is Body M directly beneath the label and the two are one block. A bare number there would be the app implying a conclusion it is not entitled to.

### 11.7 The thumbnail

**The app stores photographs of documents and bills and shows none of them.** A documents screen carrying actual images of the person's own paper is transformed by one change, and it is real content rather than decoration.

**Geometry.** Square, 8dp radius, already a token in 4.2. Three sizes: 40dp inside a dense row, 64dp inside a card, and the gallery cell, which is one third of the content width, 112dp at 393dp.

**Content.** The stored attachment, center cropped. **An attachment that is not an image renders its own kind drawing from the 5.12 set at 24dp on a `sand` field**, which keeps the whole grid in one idiom rather than dropping a generic file glyph into it.

**States.**

| State | Treatment |
|---|---|
| Loading | The `sand` field alone, no spinner. Twelve spinners is noise where twelve quiet squares is a grid still filling in |
| Present | The image, center cropped |
| Not an image | The kind drawing on `sand` |
| Unreadable | The kind drawing on `sand`, and the caption says what could not be read. Never a broken image glyph |

**A thumbnail is never the only thing naming its item**, which is 5.12's rule applied here. The caption sits below in Body M, two lines at most, with a Mono date under it. **The image does not mirror in Arabic**, because a photograph of a page is not directional. The grid order and the caption do.

**A thumbnail is generated from the stored attachment and never leaves the device**, and nothing is written outside the app's own storage. This is a local-first app and its thumbnails are held to the same rule as its data.

### 11.8 The avatar

**Initials in a tonal circle, for every person in the care team.** Twelve names in a list is a wall. Twelve names with initial marks is scannable in one pass.

**Geometry.** Circle, per 4.2. Three sizes: 32dp in a dense row, 40dp in a card, 56dp on a detail header. Initials in Label weight 700, one or two characters.

**Initials are taken by grapheme cluster, never by character index.** A name in Arabic, a name with a combining mark, and a name whose first character is an emoji all have to survive being cut to two, and cutting by code unit produces a broken glyph rather than a letter. Uppercased against the catalog's own locale, per 5.13's rule about the same mistake.

**The circle is tonal and the tone is not identity.** `sand` background with `ink2` initials. **It is never colored per person**, because a per-person color would be a second accent system and would imply a categorization this app does not have and would not be entitled to.

**One exception, recorded here rather than assumed:** the person the notebook is about carries `blue_soft` with `blue_deep` initials. There is exactly one of them, the app already spends blue on the subject at hand, and it is what tells a roster of twelve who the notebook belongs to without a label saying so.

**There are no photographs of people anywhere in this app**, and it never asks for one. It stores photographs of paper. That is a product decision as much as a visual one and it is why the avatar is initials rather than a placeholder waiting for an image.

**A person whose name was never recorded** shows the care team drawing from the 5.12 set rather than a question mark, because a question mark reads as the app asking them something.

**The avatar is decorative for a screen reader and marked so**, because the name is always beside it. An avatar announced as "avatar, K B" before every name is noise, not access.

### 11.9 The segmented control

**Two or three segments, for switching between views of the same content.** Never four.

**It switches a view. It never navigates and it never changes the subject.** If tapping it would take the person somewhere else, it is a destination and belongs in a list or a grid. That distinction is what keeps this from becoming a second, quieter navigation bar.

**Geometry.** Full width, 44dp tall inside a 48dp touch region, `sand` container at the 12dp tile radius with 4dp of inner padding. The selected segment is a `card` surface at 8dp radius carrying the label in Label weight 700 in `ink`. Unselected labels are Body M in `ink2`.

**Selection is surface plus weight, never color alone**, per 2.2, and it is the same language as the choice chip in 5.11 rather than a second one.

**Motion** is the quick 120ms from section 6, the same as chip selection, and under reduced motion it is a cut.

**It never scrolls and never wraps.** **At a font scale where the labels no longer fit, it becomes a stacked pair or trio of full width choices** rather than shrinking its text below the floor. That is the complete state for this control and it is not a fallback, it is the same control laid out for the space it has.

**When not to use it.** More than three options, which is chips 5.11. Anything that navigates. A single on or off, which is a setting and belongs in the pattern the Appearance screen already uses.

### 11.10 The inline preview

**A link that renders what it points at, rather than naming it.** One tap cheaper, and it reads as substance rather than as a cross reference.

**Four forms, and no others.** A document or a photograph, as a thumbnail 11.7 with its caption. An entry, as its own first line in Body M with its waypoint and its date, which is exactly one dense row. A person, as an avatar and a name, which is also one dense row.

**It sits in a `sand` inset at the 12dp tile radius with 12dp of padding**, so it reads as something quoted from elsewhere rather than as a peer of the content around it.

**An inset is not a card, and that is what keeps this legal.** Section 1 bans cards nested inside cards. An inset has no shadow, no elevation, and no card radius: it is the recessed surface 2.1 already defines for exactly this. A preview never carries its own shadow and never appears inside another preview.

**Tapping it opens the thing.** Long press does nothing here, because removing a link is done from the thing itself, where the person can see what they are removing.

**A preview implies the return trip**, per rule 18. If A previews B, then B carries at least a dense row for A. A one-way preview is a dead end with a picture on it.

**When not to use it.** More than three in one region, which is a list and should be built as one. Anything the person cannot actually open.

### 11.11 The trail vocabulary belongs on every screen with a sequence

Section 5.2 built waypoints, routes, spines, and distance markers, and 5.2's own diagnosis is that they sat on one screen for a week. **This is the positive instruction that follows from it.**

**Anything the person moves through in order gets a spine**: chapters, one incident's thread, a milestone arc, a medication's history, **a project's steps**, an appointment's prep sheet, the readable record, and the trail itself. They are not seven layouts. They are one shape seen seven times, and a person who learns it on the trail can read all seven.

**Anything with a distance between its events gets markers**, per 5.2.4, under the same fourteen day threshold and the same reading-direction rule.

**Anything belonging to a care thread carries that thread's route**, per 5.2.2, everywhere it appears, as a swatch beside its name.

**And the negative, which matters as much.** A set of peers with no order does not get a spine. The care team, a document gallery, a tile grid, and a list of bills are not sequences, and drawing a line down them would be decoration pretending to be a system. **A shape means the same thing everywhere it appears**, and putting a spine where there is no sequence is how that stops being true.

### 11.12 Layout patterns

**Screens must differ in structure, not only in content.** Five patterns, and every screen in this app is one of them.

**Hero plus grid.** One important thing at full size, then the rest as a tile grid under a quiet mono eyebrow, with a second compact grid below it where there is a folded set. For the notebook, Today, and More.

**Dense list.** An optional segmented control, then group headers with dense rows under them, full bleed and hairline separated. For search results, the trail, the care team, appointments, questions, standing instructions, and any long history.

**Spine.** A short header, then a spine of rows with waypoints and distance markers. For chapters, one thread, one incident, a project, a medication's history, the prep sheet, and the readable record.

**Detail with header.** Identity first: an avatar or a thumbnail at 56dp, the name in Display M, and one Mono line of the facts that identify it. Then its own facts as dense rows or one card. Then its connections as inline previews and rows. For one person, one medication, one document, one bill, one appointment, and one entry.

**Gallery.** A three column thumbnail grid with captions, grouped by month under 5.13 headers. For documents and bills.

**A screen using the same pattern as the screen it was reached from is a deliberate choice, not the default.** Opening a person from a dense list into another dense list is fine when the person's page really is a list. Opening five screens in a row that are all dense lists is the failure this section exists to end, and the question gets asked at build time rather than at review.

**Every pattern still passes 10.6 in full.** A layout pattern is where a screen starts, never what it ships as.

### 11.13 Retroactive, per rule 14

**This section applies to every screen already built, not only to new ones.** A codebase where the standard changed halfway through is a codebase with two standards, and the whole reason this document is being written before any screen is touched is so that the sweep has something definite to sweep against.

**Added to the 10.6 checklist, and checked on the device rather than in the code:**

1. No card carrying one line and a chevron. That is a dense row.
2. No list of fixed destinations rendered as rows. That is a tile grid.
3. No more than six cards in one scroll region.
4. No screen with two heroes, and no screen whose most important thing is not its largest thing.
5. No text link where the target has a thumbnail, an avatar, or a first line worth showing.
6. No number that matters rendered in body type.
7. No sequence rendered without a spine, and no spine drawn over a set of peers.
8. No screen using the same layout pattern as the screen it was reached from without a reason somebody could state.

**A screenshot that looks fine is not proof of any of this**, because it does not tell you what the screen does at another font size, in the other direction, with the year five fixture loaded, or with the keyboard up. Every screen rebuilt against this section is looked at with generated data on it, per D70.

---

## 12. Keeping this document true

With every commit, ask whether the change made anything here wrong, and fix it in the same commit. Specifically:

Any token added or changed during implementation is written back into section 2 or 4 with its measured contrast ratio. Any new component is specified in section 5 or section 11 before it is built twice, **and it is not specified until it says when to use it and when not to**, which is the omission section 11 exists to correct. Any screen that departs from the reference file gets its departure and reason added to section 3, or gets corrected to match. Any new user-facing string follows section 7, and the AI-slop ban list in section 1 gets re-checked against current research before any significant new design work.

A design document that no longer matches the software is worse than no design document, because the next session will build against it and inherit the drift.
