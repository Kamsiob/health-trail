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

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-/home/Kamsiob/Android/Sdk/platform-tools/adb}"
PACKAGE="${PACKAGE:-com.kamsiob.healthtrail}"
DB="/tmp/health-trail-$HORIZON-$SEED.db"
CONTAINER="/tmp/health-trail-$HORIZON-$SEED.htx"

echo "Generating $HORIZON, seed $SEED"
python3 "$ROOT/tools/fixtures/generate.py" --at "$HORIZON" --seed "$SEED" --out "$DB" | tail -n +2
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
sleep 8
echo "Restored. Walk it with tools/walk.sh see"
