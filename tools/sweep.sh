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
#
# **Five destinations since 2026-08-18**, when notes joined the bar, #397. The
# four that were there kept their order and every one of them moved, because
# five items share the width that four did. Measured off the phone rather than
# divided out: 1080 / 5 is 216, and the bar's own padding does not put the
# centers on those boundaries.
nav() {
  case "$1" in
    today)    X=107 ;;
    notebook) X=323 ;;
    projects) X=540 ;;
    notes)    X=755 ;;
    more)     X=971 ;;
  esac
  "$ADB" shell input tap "$X" 2302
  sleep 2
}

shot() {
  if "$ROOT/tools/screenshot.sh" "$PREFIX-$1" >/dev/null 2>&1; then
    echo "  $1"
  else
    echo "  $1  FAILED"
  fi
}

# Is this word on the screen right now? **The only honest way to ask where the
# phone is.** Everything below used to assume that a tap on the navigation bar
# had landed, and when it had not the sweep captured thirteen screenshots of
# whatever was actually in front of it and named them after the screens it
# meant to visit. A capture under the wrong name is worse than a missing one.
showing() {
  "$ADB" shell uiautomator dump /sdcard/sweep.xml >/dev/null 2>&1 || return 1
  "$ADB" shell cat /sdcard/sweep.xml 2>/dev/null | grep -qi -- "$1"
}

# Back out to a screen that actually has the navigation bar on it.
#
# **Every tap on the bar assumes the bar is there**, and the four destinations
# are tapped by position rather than by word. Start the sweep on a detail screen
# and x=107,y=2302 is not the Today tab, it is whatever row of the open list
# happens to sit there: one run opened a trail entry, pressed back into the
# trail, opened the same entry again, and reported all four destinations lost.
# **A position is only a destination once the bar is under it.**
#
# The bar is the only place `Today` and `Projects` are both on screen.
at_home() {
  "$ADB" shell uiautomator dump /sdcard/sweep.xml >/dev/null 2>&1 || return 1
  "$ADB" shell cat /sdcard/sweep.xml 2>/dev/null |
    grep -q 'text="Today"' || return 1
  "$ADB" shell cat /sdcard/sweep.xml 2>/dev/null | grep -q 'text="Projects"'
}

go_home() {
  for _ in 1 2 3 4 5 6 7 8; do
    at_home && return 0
    "$ADB" shell input keyevent KEYCODE_BACK
    sleep 1
  done
  at_home
}

# Put the list back where a search can start from, because **`KEYCODE_BACK`
# returns the notebook at the top**. That is what made this tool lie: the route
# scrolled once, captured the one section that came into view, went back, and
# then reported the remaining seven as missing because they were below the fold
# again. One scroll for thirteen rows only ever finds the first of them.
#
# **Inside the list and slowly.** Five fast flings that ended level with the
# navigation bar put the phone on a screen nobody asked for, and the sweep after
# it captured the restore screen four times under four different names. A scroll
# that reaches the edge of the list has nothing left to do, so three gentle ones
# are as good as five hard ones and cannot be read as anything but a scroll.
to_top() {
  for _ in 1 2 3; do
    "$ADB" shell input swipe 540 900 540 1900 400
    sleep 1
  done
}

# Stand on the notebook, whatever happened on the way here. Returns 1 rather
# than capturing a lie.
on_notebook() {
  for _ in 1 2 3; do
    showing 'Search everything' && return 0
    go_home || return 1
    nav notebook
    to_top
  done
  showing 'Search everything'
}

# Tap a row by its words and prove we arrived, so a route that drifts says so
# rather than capturing the same screen eleven times under eleven names.
#
# **It scrolls until it finds the row rather than assuming where the fold is.**
# The fold moves with the fixture, the font scale and the number of sections,
# so a fixed number of swipes is a number that is right today and wrong the
# next time somebody adds a section.
into() {
  local word="$1" name="$2" tries=0
  if ! on_notebook; then
    echo "  $name  LOST: not on the notebook"
    return 1
  fi
  while [ "$tries" -lt 6 ]; do
    if "$ROOT/tools/walk.sh" tap "$word" >/dev/null 2>&1; then
      sleep 2
      # **Prove we left the notebook before calling this a capture.** A tap
      # that matched a word and opened nothing used to produce a screenshot of
      # the notebook filed under the section's name.
      if showing 'Search everything'; then
        # **Found but not opened, which is what a row half behind the floating
        # button does.** uiautomator reports a partly visible node with real
        # bounds, and the tap on its center lands on the button or off the
        # screen. Scrolling it into the clear and trying again is the fix;
        # giving up here is what filed a screenshot of the notebook under the
        # name of the care threads.
        "$ADB" shell input swipe 540 1800 540 1200 300
        sleep 1
        tries=$((tries + 1))
        continue
      fi
      shot "$name"
      "$ADB" shell input keyevent KEYCODE_BACK
      sleep 2
      to_top
      return 0
    fi
    "$ADB" shell input swipe 540 1800 540 900 300
    sleep 1
    tries=$((tries + 1))
  done
  echo "  $name  NOT FOUND: $word"
  to_top
  return 1
}

echo "Capturing as $PREFIX-*"

# **Each destination proves it arrived before it is captured.** The words are
# the ones only that screen carries.
dest() {
  local tab="$1" name="$2" proof="$3"
  if ! go_home; then
    echo "  $name  LOST: no navigation bar to tap"
    return 1
  fi
  nav "$tab"
  if ! showing "$proof"; then
    "$ADB" shell input keyevent KEYCODE_BACK
    sleep 1
    nav "$tab"
  fi
  if showing "$proof"; then
    shot "$name"
  else
    echo "  $name  LOST: $tab did not open"
  fi
}

dest today    today    's day'
dest projects projects 'under way'
dest more     more     'what this app is'

dest notebook notebook 'Search everything'
to_top
for pair in \
  "Care team:careteam" \
  "Medications:medications" \
  "Appointments:appointments" \
  "Chapters:chapters" \
  "Care threads:threads" \
  "The trail:trail" \
  "Progress:progress" \
  "Documents:documents" \
  "Money:money" \
  "Instructions:instructions" \
  "Ask next time:questions" \
  "Emergency card:emergency"
do
  into "${pair%%:*}" "${pair##*:}" || true
done

echo
echo "Done. Look at them:"
ls -1 "$ROOT/docs/screenshots/$PREFIX-"*.png 2>/dev/null | sed "s|$ROOT/||"
