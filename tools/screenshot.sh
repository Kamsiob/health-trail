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

# What the device is actually showing. The label comes from here, never from
# the argument, so the filename cannot disagree with the image.
night="$("$ADB" shell cmd uimode night 2>/dev/null | tr -d '\r')"
case "$night" in
  *yes*) theme="dark" ;;
  *no*)  theme="light" ;;
  *)
    echo "Cannot tell what theme the device is in: $night" >&2
    echo "Set it explicitly on the device, then capture again." >&2
    exit 1
    ;;
esac

if [ -n "$expected" ] && [ "$expected" != "$theme" ]; then
  echo "Refusing to capture: asked for $expected, device is in $theme." >&2
  echo "Change the device theme rather than the filename." >&2
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
