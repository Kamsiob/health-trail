#!/usr/bin/env bash
# Walk the app on the phone the way a person does: read the screen, tap a thing.
#
# Rule 21 says to install it, open it, and use it before closing anything, and
# every defect worth finding tonight was found that way rather than by reading
# code. This is the tooling that made it cheap, and it lived in a session
# scratchpad for two nights, which meant every session rebuilt it from scratch.
#
#   tools/walk.sh see                 every piece of text on screen, in order
#   tools/walk.sh tap "Medications"   tap the first node whose text matches
#   tools/walk.sh fields              the editable fields and their bounds
#
# **`see` is the honest way to read a Compose screen.** It asks the semantics
# tree through uiautomator, which is what a screen reader walks. Do not reach
# for a screenshot to find out what a screen says: D68 records an entire
# afternoon lost to `uiautomator dump` reporting the view tree rather than the
# merged semantics tree, and a screenshot cannot tell you what is labeled.
#
# **`tap` matches on text or content description, case insensitively, first
# hit wins.** It prints what it tapped and where, so a walk that goes wrong
# says so rather than silently tapping the wrong row. When it prints NOT FOUND
# the thing is usually below the fold: scroll first.
#
# **A dump costs about 2.7 seconds on a five year notebook**, per #142. That is
# fine for walking and far too slow for timing anything.
#
# Kamsiob, AGPL-3.0.
set -euo pipefail

ADB="${ADB:-/home/Kamsiob/Android/Sdk/platform-tools/adb}"
PACKAGE="${PACKAGE:-com.kamsiob.healthtrail}"
DUMP=/tmp/health-trail-walk.xml

usage() {
    sed -n '3,25p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
}

dump() {
    "$ADB" shell uiautomator dump /sdcard/walk.xml >/dev/null 2>&1
    "$ADB" shell cat /sdcard/walk.xml > "$DUMP" 2>/dev/null
}

case "${1:-}" in
    see)
        dump
        tr '>' '\n' < "$DUMP" \
            | grep -o 'text="[^"]*"' \
            | grep -v 'text=""' \
            | sed 's/text=//'
        ;;

    tap)
        [ $# -ge 2 ] || usage
        dump
        python3 - "$2" "$ADB" <<'PY'
import re
import subprocess
import sys

want, adb = sys.argv[1], sys.argv[2]
xml = open('/tmp/health-trail-walk.xml', encoding='utf-8', errors='replace').read()

for match in re.finditer(r'<node[^>]*>', xml):
    node = match.group(0)
    label = (re.search(r'text="([^"]*)"', node) or [None, ''])[1]
    desc = (re.search(r'content-desc="([^"]*)"', node) or [None, ''])[1]
    if want.lower() in label.lower() or (desc and want.lower() in desc.lower()):
        bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        x = (int(bounds.group(1)) + int(bounds.group(3))) // 2
        y = (int(bounds.group(2)) + int(bounds.group(4))) // 2
        subprocess.run([adb, 'shell', 'input', 'tap', str(x), str(y)], check=False)
        print(f'tapped "{label or desc}" at {x},{y}')
        break
else:
    # **Says so rather than failing quietly.** A tap that matched nothing and
    # said nothing is how a walk ends up reporting on a screen it never reached.
    print(f'NOT FOUND: {want}. It may be below the fold; scroll and try again.')
    sys.exit(1)
PY
        ;;

    fields)
        dump
        python3 - <<'PY'
import re

xml = open('/tmp/health-trail-walk.xml', encoding='utf-8', errors='replace').read()
for match in re.finditer(r'<node[^>]*class="android\.widget\.EditText"[^>]*>', xml):
    node = match.group(0)
    text = (re.search(r'text="([^"]*)"', node) or [None, ''])[1]
    bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
    x = (int(bounds.group(1)) + int(bounds.group(3))) // 2
    y = (int(bounds.group(2)) + int(bounds.group(4))) // 2
    print(f'{text!r:40} tap {x} {y}')
PY
        ;;

    *)
        usage
        ;;
esac
