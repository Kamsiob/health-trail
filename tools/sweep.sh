#!/usr/bin/env bash
# Capture every screen in one run, so a visual audit costs one command.
#
# **This is the tool the rebuild needed and did not have.** Rule 21 says look at
# it on the phone, and the loop for that was: change one thing, reinstall,
# reseed through the restore screen, tap your way back to the screen, capture,
# look. Ninety seconds per screen, so a pass over the app was an hour and
# nobody did one. The result was defects found one at a time, in the order they
# happened to be looked at, and three separate sessions each rediscovering that
# the same card was wrong.
#
# **Seed once, walk once.** The seed is the expensive part and the app's own
# state does not change while capturing, so it happens once at the top.
#
#   tools/sweep.sh                    # every screen, light, at the baseline
#   tools/sweep.sh --no-seed          # reuse whatever is already on the phone
#   tools/sweep.sh --prefix before    # name the run, for a before and after
#
# **It never changes a phone setting.** Rule 19 allows font scale, animation and
# the reader to be changed only with the prior value recorded and restored, and
# a capture sweep has no business doing that unattended. Dark, maximum font and
# the reader are their own passes, run deliberately.
#
# Kamsiob, AGPL-3.0.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-/home/Kamsiob/Android/Sdk/platform-tools/adb}"
PKG="com.kamsiob.healthtrail"

SEED=1
PREFIX="sweep"
for a in "$@"; do
  case "$a" in
    --no-seed) SEED=0 ;;
    --prefix) shift ;;
    *) PREFIX="$a" ;;
  esac
done

"$ADB" devices | grep -q "device$" || { echo "No phone attached." >&2; exit 1; }

if [ "$SEED" = 1 ]; then
  echo "Seeding once."
  "$ROOT/tools/device.sh" year2 6 walk-appointment --appointment-on "$(date -d '+1 day' +%Y-%m-%d)" >/dev/null
fi

# The four destinations, by position rather than by word. **`walk.sh tap`
# matches the first node containing the word**, and the capture button is
# described "Add something to the notebook", so tapping "Notebook" opens the
# capture sheet instead. `docs/TRAPS.md`.
nav() {
  case "$1" in
    today)    X=133 ;;
    notebook) X=400 ;;
    projects) X=670 ;;
    more)     X=940 ;;
  esac
  "$ADB" shell input tap "$X" 2252
  sleep 2
}

shot() {
  if "$ROOT/tools/screenshot.sh" "$PREFIX-$1" >/dev/null 2>&1; then
    echo "  $1"
  else
    echo "  $1  FAILED"
  fi
}

# Tap a row by its words and prove we arrived, so a route that drifts says so
# rather than capturing the same screen eleven times under eleven names.
into() {
  "$ROOT/tools/walk.sh" tap "$1" >/dev/null 2>&1 || { echo "  $2  NOT FOUND: $1"; return 1; }
  sleep 2
  shot "$2"
  "$ADB" shell input keyevent KEYCODE_BACK
  sleep 2
}

echo "Capturing as $PREFIX-*"

nav today;    shot today
nav projects; shot projects
nav more;     shot more

nav notebook; shot notebook
for pair in \
  "Care team:careteam" \
  "Medications:medications" \
  "Appointments:appointments" \
  "Chapters:chapters" \
  "SCROLL:SCROLL" \
  "Care threads:threads" \
  "The trail:trail" \
  "Progress:progress" \
  "Documents:documents" \
  "Money:money" \
  "Standing instructions:instructions" \
  "Ask next time:questions" \
  "Emergency card:emergency"
do
  # **Half the sections are below the fold on a real notebook**, so the route
  # scrolls once rather than reporting six screens as missing. Found the first
  # time this ran.
  if [ "$pair" = "SCROLL:SCROLL" ]; then
    "$ADB" shell input swipe 540 1800 540 700 300
    sleep 1
    continue
  fi
  into "${pair%%:*}" "${pair##*:}" || true
done

echo
echo "Done. Look at them:"
ls -1 "$ROOT/docs/screenshots/$PREFIX-"*.png 2>/dev/null | sed "s|$ROOT/||"
