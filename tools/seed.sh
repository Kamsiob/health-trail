#!/usr/bin/env bash
# Put a generated notebook on the phone, through the app's own restore screen.
#
#   tools/seed.sh month6 6 walk-month-six
#   tools/seed.sh year5  5 walk-year-five
#
# Arguments: the horizon, the seed, and the passphrase to lock the container
# with. All three default to the month six notebook this project walks most.
#
# **It goes in through Restore from a file rather than by pushing a database.**
# That is the whole point and it is D61's fix used in the other direction: a
# fixture that arrives any other way has never been through the importer, and
# the first time this ran it broke journey six at the last step within minutes
# and surfaced three fixture defects that were invisible in a database file.
#
# **`adb shell pm clear` first, deliberately.** Restoring onto a notebook that
# already holds a subject is a different code path and not the one being set
# up here.
#
# Kamsiob, AGPL-3.0.
set -euo pipefail

HORIZON="${1:-month6}"
SEED="${2:-6}"
PASSPHRASE="${3:-walk-month-six}"
# **Anything else goes straight to the generator.** Grid screens 09 and 10 are
# a second situation's starting hand and a notebook with nothing outstanding,
# and neither is a horizon: they are the same history in a different state.
#
#   tools/seed.sh month6 6 walk-quiet --quiet
#   tools/seed.sh month6 6 walk-home --situation home_family
#
# **Shifted only when there is something to shift, and empty arguments are
# dropped.** `device.sh` expands an empty array to one empty string, so a plain
# `tools/device.sh` arrived here with an argument that was not one, and the
# generator refused it. `set -u` also makes an empty array expansion an error,
# hence the guard on the expansion below.
if [ "$#" -gt 3 ]; then shift 3; else set --; fi
EXTRA=()
for a in "$@"; do [ -n "$a" ] && EXTRA+=("$a"); done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-/home/Kamsiob/Android/Sdk/platform-tools/adb}"
PACKAGE="${PACKAGE:-com.kamsiob.healthtrail}"
VARIANT="$(printf '%s' "${EXTRA[*]-}" | tr -c 'a-zA-Z0-9' '-' | sed 's/-*$//')"
DB="/tmp/health-trail-$HORIZON-$SEED$VARIANT.db"
CONTAINER="/tmp/health-trail-$HORIZON-$SEED$VARIANT.htx"

echo "Generating $HORIZON, seed $SEED"
python3 "$ROOT/tools/fixtures/generate.py" --at "$HORIZON" --seed "$SEED" --out "$DB" ${EXTRA[@]+"${EXTRA[@]}"} | tail -n +2
python3 "$ROOT/tools/fixtures/pack.py" --db "$DB" --out "$CONTAINER" --passphrase "$PASSPHRASE" >/dev/null

"$ADB" push "$CONTAINER" /sdcard/Download/seed.htx >/dev/null
"$ADB" shell pm clear "$PACKAGE" >/dev/null
"$ADB" shell am start -n "$PACKAGE/.MainActivity" >/dev/null 2>&1
sleep 6

walk() { "$ROOT/tools/walk.sh" tap "$1" >/dev/null 2>&1 || true; sleep "${2:-3}"; }

# Each gate is taken only if it is there, so this works from a cleared install
# and from one that is already past setup.
walk "I understand"
walk "Skip for now"
walk "Not sure yet" 4
walk "More"
walk "Restore from a file"
walk "Choose a file" 5

# The system file picker is not the app, so it is not in the app's semantics
# tree and walk.sh cannot see it by label alone.
"$ADB" shell uiautomator dump /sdcard/walk.xml >/dev/null 2>&1
"$ADB" shell cat /sdcard/walk.xml > /tmp/health-trail-walk.xml 2>/dev/null
python3 - "$ADB" <<'PY'
import re
import subprocess
import sys
import time

adb = sys.argv[1]


def dump():
    subprocess.run([adb, 'shell', 'uiautomator', 'dump', '/sdcard/walk.xml'],
                   check=False, capture_output=True)
    out = subprocess.run([adb, 'shell', 'cat', '/sdcard/walk.xml'],
                         check=False, capture_output=True)
    return out.stdout.decode('utf-8', 'replace')


def find(xml, attr, value):
    """The middle of the first node whose attr equals value, or None."""
    for match in re.finditer(r'<node[^>]*>', xml):
        node = match.group(0)
        found = re.search(attr + r'="([^"]*)"', node)
        if found and found.group(1) == value:
            b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
            if b:
                return ((int(b.group(1)) + int(b.group(3))) // 2,
                        (int(b.group(2)) + int(b.group(4))) // 2)
    return None


def tap(point):
    subprocess.run([adb, 'shell', 'input', 'tap', str(point[0]), str(point[1])],
                   check=False)
    time.sleep(2)


xml = dump()
spot = find(xml, 'text', 'seed.htx')

# **A phone that has never opened the picker lands on Recent, not Downloads.**
# The old device had opened it dozens of times and came up where the file was,
# so this navigation was never needed until a fresh Pixel 8 arrived on
# 2026-08-12 and the seed failed with "not in the picker" on a device that
# plainly had it. The drawer, then Downloads, then look again.
if spot is None:
    roots = find(xml, 'content-desc', 'Show roots')
    if roots is not None:
        tap(roots)
        downloads = find(dump(), 'text', 'Downloads')
        if downloads is not None:
            tap(downloads)
            spot = find(dump(), 'text', 'seed.htx')

if spot is None:
    print('seed.htx is not in the picker, and Downloads could not be reached.')
    sys.exit(1)

tap(spot)
print('selected seed.htx')
PY

sleep 5
# **The field itself, by its whole label.** This used to tap the hint sentence,
# which worked only because the hint was drawn inside the field. D189 moved
# hints under their fields, where tapping one focuses nothing, and the wording
# changed with it. `=` is an exact match, because the screen's own heading
# contains the word "password" and comes first in the tree. 2026-08-17.
"$ROOT/tools/walk.sh" tap "=Password" >/dev/null 2>&1 || true
sleep 1
"$ADB" shell input text "$PASSPHRASE"
sleep 2
"$ADB" shell input keyevent 111
sleep 2

# **Everything from here down is new on 2026-08-03, and until then this script
# printed "Restored" having never tapped a restore button.** It typed the
# passphrase, dismissed the keyboard, slept, and said it was done. The notebook
# it claimed to have loaded was whatever was there before, which for a cleared
# install is nothing at all, and every screen looked at afterward was looked at
# against an empty notebook while the log said year five. That is D68 for the
# sixth time: a tool reporting on something other than what it did.
walk "Open it" 4

# **The phone's password manager offers to save the passphrase and takes the
# focus**, which is what actually stopped the taps below from ever being
# written. It is not part of this app and it is dismissed rather than answered.
walk "No thanks" 2

# **The restore screen asks which of two things to do, since #211**, and the
# confirm button is disabled and says "Choose one of these first" until it is
# answered. This script tapped the button's old label and got nothing, so the
# seed reported failure and the notebook stayed empty. That is the cost of
# automating a screen: a choice added for a person is a step added here.
#
# **Replace, always.** A seed is a notebook being put in place, not two being
# combined, and merging into whatever was there before would make the fixture
# depend on what the last run left.
walk "Replace what is here" 2

# **The confirm button is below the fold and taps do not scroll to it.** The
# review screen grew: a file summary, two options with three lines of
# explanation each, and the warning paragraph, which together push the button
# past the bottom of the viewport on a 1080x2400 phone. A tap at a location
# that is not on screen does nothing and reports nothing, so every run since
# ended with "The restore did not finish" and an empty notebook, and the
# screenshots taken afterward were of an app with no data in it.
#
# **The app is fine and was checked by hand**: scrolled, tapped, and it said
# "Restored." This is the script failing to reach a control, not a control
# that cannot be reached.
"$ADB" shell input swipe 540 1600 540 900 300
sleep 1

walk "Replace everything with this" 10

# **It says what happened rather than assuming it.** A seed script that cannot
# tell a loaded notebook from an empty one is worse than no seed script,
# because every screenshot taken afterward inherits the lie.
if "$ROOT/tools/walk.sh" see 2>/dev/null | grep -q "Restored."; then
  echo "Restored. Walk it with tools/walk.sh see"
else
  echo "The restore did not finish. What is on screen now:" >&2
  "$ROOT/tools/walk.sh" see >&2 || true
  exit 1
fi
