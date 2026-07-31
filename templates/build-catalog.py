#!/usr/bin/env python3
"""
Builds CATALOG.md from the canonical JSON in data/.

The JSON is the single source of truth. Never hand edit CATALOG.md.
Edit data/*.json and run: python3 build-catalog.py
"""
import json
import pathlib

HERE = pathlib.Path(__file__).parent
DATA = HERE / "data"

SECTION_LABELS = {
    "today": "Today", "care_team": "Care team", "medications": "Medications",
    "appointments": "Appointments", "chapters": "Chapters", "threads": "Care threads",
    "trail": "The trail", "progress": "Progress", "documents": "Documents",
    "money": "Money", "standing_instructions": "Standing instructions",
    "ask_next_time": "Ask next time", "emergency_card": "Emergency card",
    "projects": "Projects",
}


def sections(ids):
    return ", ".join(SECTION_LABELS.get(i, i) for i in ids)


def main():
    situations = json.loads((DATA / "situations.json").read_text())
    projects = json.loads((DATA / "projects.json").read_text())
    pi = json.loads((DATA / "progress-and-instructions.json").read_text())

    out = []
    w = out.append

    w("# The Care Notebook Template Catalog\n")
    w("A free, editable set of starting points for anyone keeping track of another "
      "person's care.\n")
    w("Built for the United States. Written in plain language. Free to copy, "
      "translate, and change.\n")
    w("---\n")
    w("## How to use this\n")
    w("Every template here is a **starting point, not a rule**. Care never goes the "
      "way a form expects. Take what helps, delete the rest, and add whatever your "
      "situation actually needs.\n")
    w("Nothing in this catalog is medical or legal advice. It does not tell you what "
      "care someone should get or what your rights are in your state. It helps you "
      "write down what happened, who you talked to, and what you were told.\n")
    w("**A note about where you live.** Some of these processes are the same "
      "everywhere in the country. Others work completely differently from one state "
      "to the next. Where that is true, the template says so.\n")
    w(f"**License.** {situations['content_license']}. Use it, change it, share it, "
      "including commercially. Keep the same license on what you share and credit "
      "the source.\n")
    w("---\n")

    # ---------- contents ----------
    w("## What is in here\n")
    w(f"- **{len(situations['templates'])} care settings**, each with the people to "
      "track, what runs in parallel, and a first week checklist")
    w(f"- **{len(projects['templates'])} long processes**, the applications, appeals, "
      "and paperwork fights, broken into steps")
    w(f"- **{len(pi['progress_presets'])} things to measure**, with sensible units and "
      "how often")
    w(f"- **{len(pi['standing_instructions'])} standing instructions**, the rules you "
      "give a facility, marked by whether federal rules back them up\n")
    w("---\n")

    # ---------- situations ----------
    w("# Part one: care settings\n")
    w("Pick the one closest to your situation. Each sets up who to write down, what "
      "is running at the same time, and what to do in the first days.\n")
    for t in situations["templates"]:
        w(f"## {t['name']}\n")
        w(f"*{t['subtitle']}*\n")
        w(f"**What makes this hard.** {t['burden']}\n")
        if t.get("state_variance"):
            w(f"> {projects['posture']['state_variance']}\n")
        w("**People to write down**\n")
        for r in t["roles"]:
            w(f"- {r['label']}")
        w("")
        w("**Running at the same time**\n")
        for th in t["threads"]:
            w(f"- {th['label']}")
        w("")
        w("**First days checklist**\n")
        for i, c in enumerate(t["checklist"], 1):
            w(f"{i}. {c}")
        w("")
        w("**Papers worth having a photo of**\n")
        for d in t["documents"]:
            w(f"- {d}")
        w("")
        w(f"**Keep in front:** {sections(t['forward'])}  ")
        w(f"**Tuck away until needed:** {sections(t['folded'])}\n")
        w("---\n")

    # ---------- projects ----------
    w("# Part two: the long processes\n")
    w("Applications, appeals, and paperwork fights. These run for months and involve "
      "people who do not talk to each other. Each one below is a sequence you can "
      "work through and check off.\n")
    for t in projects["templates"]:
        w(f"## {t['name']}\n")
        w(f"*{t['subtitle']}*\n")
        if t.get("state_variance"):
            w(f"> {projects['posture']['state_variance']}\n")
        if t.get("rights_note"):
            w(f"**Worth knowing.** {t['rights_note']}\n")
        if t.get("advice_note"):
            w(f"**Careful here.** {t['advice_note']}\n")
        w(f"**How long this usually takes.** {t['timeline_shape']}\n")
        w("**Who is involved**\n")
        for r in t["roles"]:
            w(f"- {r['label']}")
        w("")
        w("**Steps**\n")
        for i, s in enumerate(t["steps"], 1):
            w(f"{i}. {s}")
        w("")
        w("**Papers to gather**\n")
        for d in t["documents"]:
            w(f"- {d}")
        w("")
        w("**What you end up waiting on**\n")
        for p in t["waiting_on_prompts"]:
            w(f"- {p}")
        w("")
        w("**Where this usually goes wrong**\n")
        for f in t["failure_points"]:
            w(f"- {f}")
        w("")
        w("---\n")

    # ---------- progress ----------
    w("# Part three: things to measure\n")
    w("Only track what someone actually asked you to track, or what you personally "
      "want to see over time. Everything here records numbers and observations. "
      "Nothing here tells you whether a number is good or bad. That is a "
      "conversation for a clinician.\n")
    for p in pi["progress_presets"]:
        w(f"## {p['name']}\n")
        units = ", ".join(p["unit_options"]) if p["unit_options"] else "no units, "\
            "just notes"
        w(f"- **Units:** {units}")
        w(f"- **How often:** {p['cadence']}")
        w(f"- **What you write down:** " + ", ".join(
            f.replace('_', ' ') for f in p["fields"]))
        if p.get("help_needed_options"):
            w("- **Choices:** " + ", ".join(p["help_needed_options"]))
        if p.get("how_much_options"):
            w("- **Choices:** " + ", ".join(p["how_much_options"]))
        if p["medication_markers"]:
            w("- **Worth marking medication starts on this one**")
        if p.get("notes"):
            w(f"- **Note:** {p['notes']}")
        w("")
    w("---\n")

    # ---------- instructions ----------
    w("# Part four: standing instructions\n")
    w("These are the rules you give a facility or an agency. Write down what you "
      "asked for, who you asked, when, and what they said back. Then you have a "
      "record instead of a memory.\n")
    tags = pi["standing_instruction_tags"]
    w(f"**{tags['federal']['label']}.** {tags['federal']['explainer']}\n")
    w(f"**{tags['request']['label']}.** {tags['request']['explainer']}\n")
    for s in pi["standing_instructions"]:
        w(f"## {s['name']}\n")
        w(f"> {s['wording']}\n")
        w(f"**{tags[s['tag']]['label']}**\n")
        w(f"{s['basis']}\n")
        w(f"**What to ask for.** {s['ask_for']}\n")
        w("---\n")

    w("## About this catalog\n")
    w("This started as the template library inside Health Trail, a free and open "
      "source care notebook app that keeps everything on your own phone. The "
      "templates are published separately, on purpose, so they are useful to anyone "
      "keeping a paper binder, a spreadsheet, or a notes app instead.\n")
    w("If a template is wrong, out of date, or missing something families need, that "
      "is worth knowing about.\n")

    text = "\n".join(out) + "\n"
    assert "\u2014" not in text, "em dash found in generated catalog"
    (HERE / "CATALOG.md").write_text(text)

    counts = (len(situations["templates"]), len(projects["templates"]),
              len(pi["progress_presets"]), len(pi["standing_instructions"]))
    print(f"CATALOG.md written: {sum(counts)} templates "
          f"({counts[0]} settings, {counts[1]} processes, {counts[2]} measures, "
          f"{counts[3]} instructions), {len(text):,} characters")


if __name__ == "__main__":
    main()
