#!/usr/bin/env bash
# Put the phone in a state where device work can actually start.
#
# **connectedAndroidTest uninstalls the app every time it finishes**, so a seed
# straight after a suite fails with one word and the next walk dumps the
# owner's home screen. That happened three times in one night before this
# existed. This does install, seed and focus in one step, and refuses rather
# than half succeeding.
#
# Usage:  tools/device.sh            # install if missing, seed, focus
#         tools/device.sh --seed-only
#
# Kamsiob, AGPL-3.0.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-/home/Kamsiob/Android/Sdk/platform-tools/adb}"
PKG="com.kamsiob.healthtrail"
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"

if ! "$ADB" devices | grep -q "device$"; then
  echo "No phone attached. adb devices shows nothing usable." >&2
  exit 1
fi

# A shade or another app holding focus is why a seed cannot reach the picker.
"$ADB" shell cmd statusbar collapse >/dev/null 2>&1 || true
"$ADB" shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
sleep 1

SEED_ARGS=()
for a in "$@"; do [ "$a" = "--seed-only" ] || SEED_ARGS+=("$a"); done

if [ "${1:-}" != "--seed-only" ]; then
  if ! "$ADB" shell pm list packages | grep -q "$PKG"; then
    echo "The app is not installed, which is what an instrumented run leaves behind."
    [ -f "$APK" ] || { echo "No debug APK at $APK. Build it first." >&2; exit 1; }
    "$ADB" install -r "$APK" | tail -1
  fi
fi

"$ROOT/tools/seed.sh" "${SEED_ARGS[@]:-}" | tail -2

"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
sleep 3

focus="$("$ADB" shell dumpsys window 2>/dev/null | grep -m1 'mCurrentFocus' || true)"
case "$focus" in
  *"$PKG"*) echo "Ready. The app is installed, seeded and focused." ;;
  *) echo "The app is installed and seeded but is not focused: $focus" >&2
     echo "Walking or capturing now would read whatever is on the phone." >&2
     exit 1 ;;
esac
