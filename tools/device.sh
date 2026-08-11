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
  [ -f "$APK" ] || { echo "No debug APK at $APK. Build it first." >&2; exit 1; }

  # **Always, rather than only when the package is missing.** Installing only
  # when absent meant a phone that already had the app kept whatever build was
  # on it, so a walk after a source change walked the change that was not
  # there. It reads as the fix not working, which is the most expensive way to
  # be wrong: the next move is to go and break the fix that was correct.
  # `install -r` keeps the data, and the seed below replaces it anyway.
  #
  # **And the APK is only as new as the last assemble.** compileDebugKotlin
  # does not build one, so a session that compiles, installs and walks is
  # walking the build before its own change. Said out loud rather than left to
  # a timestamp nobody reads.
  NEWEST="$(find "$ROOT/android/app/src/main" -name '*.kt' -newer "$APK" -print -quit 2>/dev/null || true)"
  if [ -n "$NEWEST" ]; then
    echo "The APK is older than the source. Run: (cd android && ./gradlew assembleDebug)" >&2
    echo "  first source file newer than the APK: ${NEWEST#"$ROOT"/}" >&2
    exit 1
  fi

  "$ADB" install -r "$APK" | tail -1
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
