#!/usr/bin/env bash
# Capture a screenshot of Health Trail from a connected device or emulator.
#
# The foreground check below is not politeness, it is a safety mechanism. This
# is the owner's daily driver phone. A mistimed capture would put his personal
# content into a public repository, and that cannot be prevented by being
# careful about when the script is run. So the script refuses to capture unless
# this application is genuinely the focused window, and it checks immediately
# before the capture rather than at the start.
#
# The theme in the filename is read from the device rather than taken on trust.
# A capture labeled light that is actually dark is a lie in a public repository,
# and it is the kind of lie nobody catches, because the label is believed and
# the image is only glanced at. So the label is derived, and an argument that
# disagrees with the device is refused rather than honored.
#
# Usage:
#   tools/screenshot.sh <name> [light|dark]
#
# Writes to docs/screenshots/<name>-<theme>.png
#
# Kamsiob, AGPL-3.0.

set -euo pipefail

PACKAGE="com.kamsiob.healthtrail"
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

name="${1:-}"
expected="${2:-}"

if [ -z "$name" ]; then
  echo "Usage: tools/screenshot.sh <name> [light|dark]" >&2
  exit 2
fi

if [ ! -x "$ADB" ]; then
  echo "adb not found at $ADB. Set ADB to its path." >&2
  exit 1
fi

if [ "$("$ADB" devices | grep -c "device$")" -eq 0 ]; then
  echo "No device or emulator connected." >&2
  exit 1
fi

# The focused window, according to the window manager. Checked immediately
# before capture. If anything else is in front, including a notification shade,
# a permission dialog belonging to another app, or the launcher, this refuses.
focused="$("$ADB" shell dumpsys window 2>/dev/null \
  | grep -E 'mCurrentFocus|mFocusedApp' \
  | head -2 || true)"

if ! echo "$focused" | grep -q "$PACKAGE"; then
  echo "Refusing to capture: $PACKAGE is not the focused window." >&2
  echo "" >&2
  echo "Currently focused:" >&2
  echo "$focused" >&2
  echo "" >&2
  echo "This device is the owner's daily driver. A capture taken while" >&2
  echo "something else is in front would put his personal content into a" >&2
  echo "public repository." >&2
  exit 1
fi

# Focus is not enough, and this was learned the expensive way.
#
# On 2026-08-01 a capture of a fully focused Health Trail came back with an
# incoming call banner across the top of it, carrying a phone number and a
# contact photo belonging to the owner. **A heads-up notification never takes
# focus**, so every check above passed and the image was wrong anyway. It was
# caught by looking at the picture, which is not a control.
#
# So heads-up notifications are switched off for the duration of the capture
# and switched back immediately. The trap restores the previous value on every
# exit path, including a failure, a refusal, and an interrupt, because leaving
# somebody's daily driver silent is its own kind of damage.
heads_up_before="$("$ADB" shell settings get global heads_up_notifications_enabled 2>/dev/null | tr -d '\r')"

restore_heads_up() {
  if [ "$heads_up_before" = "null" ] || [ -z "$heads_up_before" ]; then
    "$ADB" shell settings delete global heads_up_notifications_enabled >/dev/null 2>&1 || true
  else
    "$ADB" shell settings put global heads_up_notifications_enabled "$heads_up_before" >/dev/null 2>&1 || true
  fi
}
trap restore_heads_up EXIT INT TERM

"$ADB" shell settings put global heads_up_notifications_enabled 0 >/dev/null 2>&1 || true

# Anything already on screen from another application, checked as well, because
# suppressing new notifications does nothing about one that is already up.
# Looks for visible windows belonging to a package that is neither this app nor
# the system chrome that is always present.
intruders="$("$ADB" shell dumpsys window windows 2>/dev/null \
  | grep -E '^\s+Window\{' \
  | grep -vE "$PACKAGE|StatusBar|NavigationBar|ScreenDecor|InputMethod|DockedStackDivider|NotificationShade u0 NotificationShade\}" \
  | grep -iE 'heads-up|HeadsUp|Toast|PopupWindow' || true)"

if [ -n "$intruders" ]; then
  echo "Refusing to capture: something is overlaying the app." >&2
  echo "$intruders" >&2
  echo "" >&2
  echo "This device is the owner's daily driver. Wait for it to clear." >&2
  exit 1
fi

# What the app is actually showing. The label is derived, never taken from the
# argument, so the filename cannot disagree with the image. D31.
#
# **The device theme is no longer the answer on its own.** Since the in-app
# Appearance setting landed, the app can be dark on a light phone and the
# reverse, so reading `cmd uimode night` alone would mislabel every capture
# where the two disagree. The app's own stored choice is asked first and the
# device is consulted only when that choice is to follow it.
#
# Read through `run-as`, which works because this is a debuggable build. A
# release build has no such setting to read and no screenshots to take.
choice="$("$ADB" shell run-as "$PACKAGE" cat shared_prefs/health-trail-appearance.xml 2>/dev/null \
  | grep -o 'name="theme_choice">[A-Z_]*' | sed 's/.*>//' || true)"

night="$("$ADB" shell cmd uimode night 2>/dev/null | tr -d '\r')"

case "$choice" in
  DARK)  theme="dark" ;;
  LIGHT) theme="light" ;;
  # FOLLOW_SYSTEM, or no preference written yet, which is the same thing.
  *)
    case "$night" in
      *yes*) theme="dark" ;;
      *no*)  theme="light" ;;
      *)
        echo "Cannot tell what theme the app is in." >&2
        echo "App choice: ${choice:-<unset, following the phone>}" >&2
        echo "Device night mode: $night" >&2
        exit 1
        ;;
    esac
    ;;
esac

if [ -n "$expected" ] && [ "$expected" != "$theme" ]; then
  echo "Refusing to capture: asked for $expected, the app is in $theme." >&2
  echo "Change the theme in the app under More, Appearance, rather than the" >&2
  echo "filename. The label is derived from what is on screen." >&2
  exit 1
fi

outdir="$ROOT/docs/screenshots"
mkdir -p "$outdir"
target="$outdir/${name}-${theme}.png"

"$ADB" exec-out screencap -p > "$target"

if [ ! -s "$target" ]; then
  echo "Capture produced an empty file." >&2
  rm -f "$target"
  exit 1
fi

# Confirm again afterward. If the foreground changed mid capture, the image is
# suspect and is discarded rather than kept and inspected by hand.
focused_after="$("$ADB" shell dumpsys window 2>/dev/null \
  | grep -E 'mCurrentFocus|mFocusedApp' | head -2 || true)"
if ! echo "$focused_after" | grep -q "$PACKAGE"; then
  rm -f "$target"
  echo "Foreground changed during capture. Image discarded." >&2
  exit 1
fi

size="$(wc -c < "$target")"
echo "Captured $target ($size bytes)"
