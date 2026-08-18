# What each tracked thing is, and what its screen owes it

The brief for #398, one entry per preset in `templates/data/progress-and-instructions.json`.

**Why this file exists.** The owner, 2026-08-18: "search online wide and deep
for each of the different things that we're going to track so that when you're
creating the material 3 expressive interface for them it lines up with the type
of thing that is being tracked. **I don't want random visuals or artifacts being
created that have absolutely nothing to do with what's being tracked.**"

**Read this before drawing any measure screen.** Each entry says what the thing
is, what shape its record actually has, what the screen shows, and **the line
rule 2 draws around it**. The last part is the one that matters most: the
published research on several of these is largely about interpretation, and
interpretation is the one thing this app never does.

---

## 0. The rule that binds every entry

**Rule 2. Record, organize, count. Never conclude.** No target range, no normal
value, no threshold, no color by value, no arrow, no trend judgment, no
severity. **This is not caution, it is the product.** Everything below was
researched with that filter on, and the research is at least as useful for what
it rules out as for what it suggests.

**Three findings from the search that apply to all sixteen:**

1. **The commercial category is built on the thing this app must not do.**
   Blood pressure apps advertise automatic classification into hypertension
   stages, category pie charts and trend regression lines as their headline
   features. Every one of those is a conclusion about somebody's health.
   `docs/V4.md` 6.1 item 11 says a screen that could belong to any app has
   failed; here the stronger claim holds, that a screen that looks like those
   apps has broken rule 2.
2. **Percentiles and scores get read as verdicts.** A 2007 study in *Pediatrics*
   found parents read the 90th percentile as "normal" and the 10th as not,
   which is the clearest published evidence that a number placed against a
   population is heard as a judgment about the person. It is why growth here is
   measurements over time and never a percentile.
3. **What clinicians write down is mostly plain fact**, and the app's presets
   already match it: date, time, place, what happened, what was done, who was
   told. The fall entry below is nearly a field-for-field match with what care
   homes are told to record.

---

## 1. Continuous, a number over time

Eight presets: weight, blood pressure, blood sugar, pain, sleep, fluids,
temperature, growth.

**The shape.** A line over time, `ui/v4/Trace.kt`, one color from the measure's
own hue, D204, **gaps drawn as gaps**. The figure is the latest reading, large,
with its unit. Under it the readings themselves in date order.

**Never**: a shaded band, a target line, a reference range, an axis marked with
anything but the person's own numbers, a color that changes with a value.

### Weight
Ordinary continuous. `medication_markers: true` matters: the preset exists partly
so somebody can see a new medication's start beside the line. **The marker is a
date somebody wrote down, never a claim of cause.**

### Blood pressure
**Two numbers in one reading, and this is the one that shapes the screen.**
Systolic and diastolic are a pair; the record also carries who took it, because
readings at home and at a clinic differ, which is the preset's own note. The
screen draws **two lines on one plot**, or two figures on one card, and it never
draws a single "blood pressure" number.

Everything the category offers beyond that is classification: stage 1, stage 2,
prehypertension, hypotension, heat maps. **None of it.** The app shows the
numbers a person was given and who took them.

### Blood sugar
The reading is meaningless without its tag: **before or after eating**, which is
already a field. The category's own convention is exactly this pairing, and
paired display is the useful part; the trend analysis sold alongside it is not.
The screen shows the tag on every reading and groups by it where somebody asks.
`advice_risk: high` in the preset, and it is right.

### Pain
**0 to 10, and it is their report, not the family's assessment**, which the
preset says in its own note. The clinical scale is a self-report instrument.
Where they cannot say, what the app records is what was observed and who
observed it, in words.

**Never** map a number to a word. The published anchors, "mild", "moderate",
"severe, disabling", are an interpretation the app does not make. Ten is ten.

Location is part of the record. A body map is the convention; **a diagram is
worth building only if it stays a place to write down where, and never becomes a
severity picture**.

### Sleep
Hours, plus times woken. Both are counts. A bar per night reads better than a
line for something counted per day, and that is a shape decision rather than an
interpretation.

### Fluids
**In and out, which is a two-direction record**, already a field. Clinical
practice charts intake and output side by side over 24 hours and totals each. The
app may total what somebody wrote down, because **a total is arithmetic**. It may
not say whether the total is enough, which is what "target fluid intake" means on
a hospital chart, and that is where the app stops.

### Temperature
Ordinary continuous, units F or C. **No fever threshold**, no color.

### Growth
Height, weight, head measurement, for a child. **No percentile, no centile
curve, no WHO or CDC overlay.** The evidence above is that percentiles are read
as marks out of a hundred, and the app would be handing a parent a verdict it
did not compute and cannot defend. The screen is the child's own measurements
over time, which is a real and useful thing to keep.

---

## 2. Categorical, one of a few answers

**Eating (appetite).** How much, per meal. The record is a small set of choices
plus a note. **The shape is not a line**, because the answers are not numbers on
a scale. A run of days as a row of marks, one per meal, reads as a pattern
without ever summing to a score.

**Never** a percentage eaten, an average, or "3 of 4 meals". Counting meals is
counting the person's diligence as much as the intake, rule 13.

---

## 3. Observational, what somebody saw

**Mood and behavior.** Fields: what you saw, date, time of day, who reported it.

**The established caregiver instrument is the ABC chart**: antecedent, behavior,
consequence. It is used in dementia care to notice what precedes a behavior,
and it is a structure for observation rather than a scale. **The app's three
fields are already an ABC chart in plain words**, and the useful addition is the
antecedent, what was happening just before.

**Never** a mood score, a rating out of five, a count of "difficult days", or a
face scale. The record is what somebody saw and who saw it.

**Bathroom.** Date and note. The clinical convention is the **Bristol stool
chart**, seven types, and documentation studies treat its use as the quality
measure. **A seven-type picker is a vocabulary, not a judgment**, and it is the
one place a small set of illustrations would be genuinely the thing being
tracked rather than decoration. It stays optional: somebody writing "went twice"
has written a complete record, rule 13.

**Never** a constipation warning, a frequency target, or a color that changes
with the answer.

---

## 4. Event log, a thing that happened

**Falls.** Fields: date, time, where it happened, what happened, what was done,
who told you, who you notified.

**This is the preset that most closely matches what the world says to record.**
Care home guidance asks for the date, time, location and circumstances, the
immediate response, whether the family and the physician were notified, and it
treats notification as a documented fact. **The app's fields already carry all of
it**, and the screen's job is to make each one easy to write in a corridor and
easy to read back a year later.

**The list is the record and the count is a fact.** "Four falls since March" is
counting, which rule 2 allows. **A falls risk score is not**, and neither is any
statement about whether the number is high.

**Dialysis sessions.** Date, attended or missed, length, how they felt after.
Missed sessions are the clinically consequential thing and the app records them
as a plain fact. **The word for a missed session is "missed", never
"noncompliance"**, which is the word the clinical literature uses and which
puts the blame in the record.

**Treatment cycles.** Cycle number, date, what you noticed after. A sequence,
numbered, with the person's own words attached. The road drawing, `RoadStrip`,
is the app's own shape for a numbered sequence and is the honest reuse here.

---

## 5. Photo log

**Wound or pressure sore.** Fields: photo, date, location on body, what the
clinician said.

**The whole category is built on timestamped photographs compared over time**,
and the professional tools add automatic area measurement and AI staging. **The
app takes the photograph and the date and stops.** The stage field records what
a clinician said, and the label must say so, which the preset already requires.

The screen is a **run of thumbnails in date order**, the person's own paper
treatment, `ui/v4/Thumbnail.kt`, with the date under each and the clinician's
words beside it.

**Never** measure the wound, stage it, compute area, or draw anything that reads
as better or worse.

---

## 6. Milestone heavy

**Moving around (mobility).** Distance or time, plus how much help was needed:
"No help", "A little help", "A lot of help", "Could not today".

**The clinical scales exist and the app deliberately does not use them.** CARE
and FIM score assistance from 1 to 6, and therapists track distance rising as
assistance falls. **The preset's own note says why**: record what the therapist
said, not your own rating. Plain words cannot be summed into a score, which is
the point.

The screen shows distance over time where somebody gave distances, and the help
level as a word on each entry. **Never** a score, never a percentage of
independence, never a line implying a direction of travel.

---

## 7. What this means for the interface, in one paragraph

Five shapes cover sixteen things: **a line for a number over time**, **a row of
marks for a categorical answer**, **a dated list for an observation**, **a
sequence for an event log**, and **a run of photographs for a wound**. Each
screen leads with what the person last wrote down, in the measure's own hue,
D204, and everything below it is the record in date order. **No screen shows a
number the person did not write down**, except a count or a total, which is
arithmetic. That is the whole system, and it is why nothing in it needs a
visual that has nothing to do with what is being tracked.

---

## Sources

- Blood pressure app conventions and what they classify: [bphealth.app best blood pressure app 2026](https://www.bphealth.app/en/blog/best-blood-pressure-app-2026), [SmartBP](https://smartbp.app/), [Cardilog](https://cardilog.app/blog/best-blood-pressure-apps)
- Wound photo documentation practice: [imito](https://imito.io/en), [PixioDoc wound care](https://pixiodoc.com/specialty/wound-care), [Swift Skin and Wound](https://swiftmedical.com/swift-skin-and-wound-how-a-smartphone-app-is-revolutionizing-wound-care/), [AI pressure injury assessment, Frontiers in Medical Technology](https://www.frontiersin.org/journals/medical-technology/articles/10.3389/fmedt.2022.905074/full)
- Fluid balance charting: [Geeky Medics fluid balance monitoring](https://geekymedics.com/fluid-balance-monitoring-osce-guide/), [Nurseslabs intake and output](https://nurseslabs.com/monitoring-fluid-intake-and-output-io/), [WTCS Nursing Assistant 7.7](https://wtcs.pressbooks.pub/nurseassist/chapter/7-7-measuring-intake-and-output/)
- Fall documentation: [AHRQ fall response, chapter 2](https://www.ahrq.gov/patient-safety/settings/long-term-care/resource/injuries/fallspx/man2.html)
- ABC behavior charts: [NHS Wales ABC chart](https://awttc.nhs.wales/files/guidelines-and-pils/abc-chart-pdf/), [DementiaHub.SG](https://www.dementiahub.sg/dementia-practice/responding-to-behaviour-changes-using-the-abc-approach/), [Trualta behavior chart at home](https://www.trualta.com/resources/blog/how-to-create-a-behavior-chart-for-dementia-care-at-home/)
- Bristol stool chart in documentation: [Bladder and Bowel Community](https://www.bladderandbowel.org/bowel/bowel-resources/bristol-stool-form-scale/), [Cureus quality improvement project](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC12874475/)
- Pain scales and body maps: [ATrain, assessing and documenting pain](https://www.atrainceu.com/content/10-assessing-and-documenting-pain), [numeric rating scale anchors, PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC8730642/)
- Glucose tagging conventions: [Glucobyte](https://glucobyte.app/), [GLog glucose logbook](https://apps.apple.com/us/app/glog-glucose-logbook/id1440799088)
- Mobility assistance levels: [CARE tool levels of assistance](https://essentialhh.org/occupational-therapy-levels-of-assistance/), [PT-led mobility documentation scoping review, PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC12733592/)
- Growth chart misinterpretation: [Why childhood growth charts are so easily misinterpreted](https://www.parent.com/blogs/conversations/2017-why-childhood-growth-charts-are-so-easily-misinterpreted), [CDC, use and interpretation of the WHO and CDC growth charts](https://www.cdc.gov/growth-chart-training/media/pdfs/2025/03/Use-of-WHO-CDC-Growth-Charts_508.pdf)
- Missed dialysis documentation and its vocabulary: [Missed treatments workbook, Alliant Health](https://quality.allianthealth.org/wp-content/uploads/2021/07/Missed-Treatments-Workbook-English_v2_508.pdf)
