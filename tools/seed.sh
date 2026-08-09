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

adb = sys.argv[1]
xml = open('/tmp/health-trail-walk.xml', encoding='utf-8', errors='replace').read()
for match in re.finditer(r'<node[^>]*>', xml):
    node = match.group(0)
    if (re.search(r'text="([^"]*)"', node) or [None, ''])[1] == 'seed.htx':
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        subprocess.run([
            adb, 'shell', 'input', 'tap',
            str((int(b.group(1)) + int(b.group(3))) // 2),
            str((int(b.group(2)) + int(b.group(4))) // 2),
        ], check=False)
        print('selected seed.htx')
        break
else:
    print('seed.htx is not in the picker. Open Downloads in the picker and retry.')
    sys.exit(1)
PY

sleep 5
walk "The one you chose when you saved this file" 1
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
