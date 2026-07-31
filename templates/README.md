# Care Notebook Templates

A free set of 57 starting points for anyone keeping track of another person's care: a parent in a nursing home, a spouse in rehab, a child in ongoing treatment.

Two ways to use this:

1. **Read the catalog.** [CATALOG.md](CATALOG.md) is the whole thing in plain language. Works with a paper binder, a spreadsheet, or a notes app. No software required.
2. **Build with the data.** [`data/`](data/) holds the same content as structured JSON, ready to embed in an app.

This is the template library from **Health Trail**, a free and open source care notebook that keeps everything on your own phone. The templates are published separately on purpose, so they are useful even to people who never install anything.

## What is in here

| | Count | What it is |
|---|---|---|
| Care settings | 14 | Nursing home, hospital, home care, rehab, hospice, dialysis, memory care, and more. Each with the people to track, what runs in parallel, and a first days checklist. |
| Long processes | 16 | Medicaid applications, coverage appeals, records requests, discharge planning, legal paperwork. Broken into steps, with what you end up waiting on. |
| Things to measure | 16 | Weight, blood pressure, mobility, wounds, falls, and others, with units and how often. |
| Standing instructions | 11 | The rules you give a facility, each marked by whether federal rules back it up or it is simply your request. |

## What this is not

- **Not medical advice.** Nothing here says what care anyone should receive. There are no target ranges, no normal values, no warnings, and no interpretation of any measurement.
- **Not legal advice.** Where federal rules are mentioned, they are named as facts. Nothing here tells you what your rights are in your state.
- **Not a substitute for a doctor, nurse, emergency services, or a lawyer.**

Everything is a starting point. Care never goes the way a form expects.

## Files

```
CATALOG.md                            the whole catalog, human readable
build-catalog.py                      regenerates CATALOG.md from data/
SCHEMA.md                             what every field in the JSON means
LICENSE-CONTENT.md                    CC BY-SA 4.0, applies to the templates
data/situations.json                  14 care settings
data/projects.json                    16 long processes
data/progress-and-instructions.json   16 measures, 11 standing instructions
```

**The JSON is the source of truth.** `CATALOG.md` is generated from it. Do not hand edit the catalog. Edit the JSON and run:

```
python3 build-catalog.py
```

## Rules the content follows

Anyone editing or translating this should hold to these, since they are the reason it stays safe to hand to a stressed family.

1. **Structure, never advice.** A template configures what to write down. It never says what to do medically or legally.
2. **Original wording only.** Every string here was written fresh. Nothing is copied from government pamphlets, ombudsman sites, law firm pages, or other apps. Structures can be mirrored. Sentences cannot.
3. **No volatile facts.** No contractor names, no dollar limits, no phone numbers, no agency names that get renamed. Generic descriptions instead, so nothing goes stale and quietly becomes wrong.
4. **Rights stated precisely.** Where federal nursing home rules back something up, it says so and names the scope. Where something is only a reasonable request, it says that instead. Overstating a right would send a family into a meeting with a claim that does not hold.
5. **Short strings, plain words.** Everything is written to translate cleanly. No idioms, no jargon, no long sentences.
6. **No em dashes.** House style.

## Translation

The content ships in English. Some labels do not survive a literal translation and need a description plus a plain explanation instead of a dictionary equivalent. The JSON flags these with `localization_note`. The known ones:

- **Hospice.** The direct equivalent in several languages means a poorhouse, an orphanage, or "care of the dying," and carries real stigma. Use comfort focused care wording.
- **Power of attorney, advance directive, health care proxy.** These are legal instruments that do not exist in the same form everywhere, and the names mislead. Describe what the document does.
- **Social worker, case manager.** These job titles map badly, and in some communities "social worker" is associated with child protection. Describe the function instead.

Any translation should be reviewed by a native speaker who has actually dealt with the American care system, not just a fluent translator.

## Contributing

If a template is wrong, out of date, or missing something families need, that is worth knowing about. Corrections from people who have lived it are the most valuable kind.

## License

Template content is licensed **CC BY-SA 4.0**. Copy it, change it, translate it, use it commercially. Keep the same license on what you share and credit the source.

`build-catalog.py` is licensed AGPLv3, matching the app it came from.
