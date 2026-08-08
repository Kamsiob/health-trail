#!/usr/bin/env python3
"""Read and update the Health Trail project board.

The board's two built-in automations, auto-add and move to Done on close, have
no API and no `gh` command. They are configurable only in the web interface, so
until the owner switches them on the board is maintained by hand. This tool is
that hand, so it is one command rather than a page of GraphQL each time, and so
a session with no memory does not have to rediscover the field and option ids.

Usage:

    python3 tools/board.py show                  everything, grouped by status
    python3 tools/board.py sync                  add missing issues, close what is closed
    python3 tools/board.py set 5 --status "In progress"
    python3 tools/board.py set 5 --status Done --actual M

`sync` is the one to run after any batch of issue work. It adds any issue not
yet on the board as Todo, and moves any item whose issue is closed to Done. It
never moves an open issue, because whether something is in progress is a
judgment the session makes, not something derivable from issue state.

Kamsiob, AGPL-3.0.
"""

import argparse
import json
import subprocess
import sys
from collections import defaultdict

OWNER = "Kamsiob"
REPO = "Kamsiob/health-trail"
PROJECT_NUMBER = 2
PROJECT_ID = "PVT_kwHOEkE_j84BfAWu"

FIELDS = ("Status", "Platform", "Area", "Priority", "Size", "Actual")

ADD_ITEM = (
    "mutation($p: ID!, $c: ID!) { addProjectV2ItemById("
    "input: {projectId: $p, contentId: $c}) { item { id } } }"
)
SET_FIELD = (
    "mutation($p: ID!, $i: ID!, $f: ID!, $o: String!) { updateProjectV2ItemFieldValue("
    "input: {projectId: $p, itemId: $i, fieldId: $f, value: {singleSelectOptionId: $o}}"
    ") { projectV2Item { id } } }"
)


def gh(args, check=True):
    result = subprocess.run(["gh"] + args, capture_output=True, text=True)
    if result.returncode != 0:
        if check:
            print(f"  gh failed: {result.stderr.strip()[:300]}", file=sys.stderr)
        return None
    return result.stdout


def mutate(query, **variables):
    args = ["api", "graphql", "-f", f"query={query}"]
    for key, value in variables.items():
        args += ["-f", f"{key}={value}"]
    raw = gh(args)
    return json.loads(raw) if raw else None


def load_fields():
    raw = gh(["project", "field-list", str(PROJECT_NUMBER), "--owner", OWNER,
              "--format", "json"])
    fields = {f["name"]: f for f in json.loads(raw)["fields"]}
    options = {
        name: {o["name"]: o["id"] for o in fields[name].get("options", [])}
        for name in FIELDS if name in fields
    }
    return fields, options


# **Above any real count, and checked rather than trusted.** Both reads used to
# ask for 200. The repository passed 200 issues, so the board read saw 200 of
# 271 items and every issue past the cap looked absent: sync re-added it, which
# GitHub treats as a no-op, and then **set its Status**, so an open issue the
# owner had marked In Progress was quietly put back to Todo by a routine sync.
# The file's own closing line says an open issue is never moved automatically,
# and for seventy of them that was not true. The issue read was capped the same
# way, so seventy-nine issues were never synced at all and eight of them were
# missing from the board entirely.
#
# Found on 2026-08-08 by noticing that `sync` reported "200 added" and then "63
# added" on an immediately repeated run. **A count that changes when nothing
# did is the tool describing something other than what it did**, which is D68
# for the umpteenth time.
PAGE = 1000


def truncated(name, count):
    """A read that came back exactly full is a read that was cut off."""
    if count >= PAGE:
        raise SystemExit(
            f"{name} returned {count} rows, which is the page size. This tool "
            f"cannot see past it and would silently treat everything beyond as "
            f"absent. Raise PAGE or paginate before trusting anything below."
        )
    return count


def load_items():
    raw = gh(["project", "item-list", str(PROJECT_NUMBER), "--owner", OWNER,
              "--format", "json", "--limit", str(PAGE)])
    items = []
    for entry in json.loads(raw)["items"]:
        content = entry.get("content") or {}
        items.append({
            "item_id": entry["id"],
            "number": content.get("number"),
            "title": content.get("title", entry.get("title", "")),
            "status": entry.get("status"),
            "priority": entry.get("priority"),
            "area": entry.get("area"),
        })
    truncated("The board item read", len(items))
    return items


def load_issues():
    raw = gh(["issue", "list", "--repo", REPO, "--state", "all",
              "--limit", str(PAGE), "--json", "number,id,title,state"])
    issues = json.loads(raw)
    truncated("The issue read", len(issues))
    return issues


def set_field(fields, options, item_id, name, value):
    if name not in options or value not in options[name]:
        print(f"  unknown {name} value {value!r}", file=sys.stderr)
        return False
    result = mutate(SET_FIELD, p=PROJECT_ID, i=item_id, f=fields[name]["id"],
                    o=options[name][value])
    return result is not None


def command_show():
    items = load_items()
    grouped = defaultdict(list)
    for item in items:
        grouped[item["status"] or "no status"].append(item)
    order = ["In progress", "Blocked", "Todo", "Done", "no status"]
    for status in order + [s for s in grouped if s not in order]:
        if status not in grouped:
            continue
        rows = sorted(grouped[status], key=lambda i: i["number"] or 0)
        print(f"\n{status} ({len(rows)})")
        for row in rows:
            marker = "!" if status == "Blocked" else " "
            print(f"  {marker} #{row['number']:<3} {row['title'][:72]}")
    in_progress = len(grouped.get("In progress", []))
    print(f"\nTotal {len(items)}. In progress: {in_progress}.")
    if in_progress > 2:
        print("  Note: more than two items in progress means the board is "
              "lying about focus. One person works on one thing.")


def command_sync():
    fields, options = load_fields()
    items = load_items()
    issues = load_issues()
    on_board = {item["number"]: item for item in items if item["number"]}

    added = moved = 0
    for issue in sorted(issues, key=lambda i: i["number"]):
        number = issue["number"]
        if number not in on_board:
            result = mutate(ADD_ITEM, p=PROJECT_ID, c=issue["id"])
            if not result:
                continue
            item_id = result["data"]["addProjectV2ItemById"]["item"]["id"]
            target = "Done" if issue["state"] == "CLOSED" else "Todo"
            set_field(fields, options, item_id, "Status", target)
            print(f"  added   #{number:<3} as {target}: {issue['title'][:60]}")
            added += 1
            continue

        item = on_board[number]
        if issue["state"] == "CLOSED" and item["status"] != "Done":
            set_field(fields, options, item["item_id"], "Status", "Done")
            print(f"  moved   #{number:<3} to Done: {issue['title'][:60]}")
            moved += 1

    print(f"\nSync done. {added} added, {moved} moved to Done.")
    print("An open issue is never moved automatically. Whether something is in "
          "progress is a judgment, not a derivation.")


def command_set(args):
    fields, options = load_fields()
    items = {item["number"]: item for item in load_items() if item["number"]}
    if args.number not in items:
        print(f"#{args.number} is not on the board. Run sync first.", file=sys.stderr)
        return 1
    item_id = items[args.number]["item_id"]
    for name in ("status", "platform", "area", "priority", "size", "actual"):
        value = getattr(args, name)
        if value:
            ok = set_field(fields, options, item_id, name.capitalize(), value)
            print(f"  #{args.number} {name} -> {value}" if ok else f"  failed: {name}")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("show", help="print the board grouped by status")
    sub.add_parser("sync", help="add missing issues, move closed ones to Done")
    setter = sub.add_parser("set", help="set fields on one item")
    setter.add_argument("number", type=int)
    for name in ("status", "platform", "area", "priority", "size", "actual"):
        setter.add_argument(f"--{name}")

    args = parser.parse_args()
    if args.command == "show":
        command_show()
    elif args.command == "sync":
        command_sync()
    elif args.command == "set":
        return command_set(args)
    return 0


if __name__ == "__main__":
    sys.exit(main())
