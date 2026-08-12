#!/usr/bin/env bash
# Run every verification this project has, and be honest about the result.
#
# This exists because an ad hoc command printed "all pass" while the build had
# actually failed. The message was written unconditionally after a grep, so it
# printed whatever happened. The exit code was never checked, and a summary was
# reported instead of the tool output.
#
# So the rules this script is built to obey:
#
#   Every step's exit code is captured and checked. Nothing is inferred from
#   whether some text appeared in the output.
#
#   A failing step never stops the run. Everything runs, so one failure does not
#   hide the state of everything after it.
#
#   The summary names every step and its real result, and the script exits
#   nonzero when any step failed, naming which.
#
#   Steps that could not run are reported as SKIPPED, never as passed. A step
#   that did not run is not a step that succeeded.
#
# Usage:
#   tools/verify.sh            everything that does not need a device
#   tools/verify.sh --device   also run the instrumented suite on the phone
#
# Kamsiob, AGPL-3.0.

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/home/linuxbrew/.linuxbrew/opt/openjdk@21}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"

WITH_DEVICE=0
[ "${1:-}" = "--device" ] && WITH_DEVICE=1

LOG_DIR="$(mktemp -d)"
declare -a NAMES RESULTS LOGS

run_step() {
  local name="$1"; shift
  local log="$LOG_DIR/$(echo "$name" | tr ' /' '__').log"
  printf '  %-42s ' "$name"
  if "$@" > "$log" 2>&1; then
    printf 'PASS\n'
    NAMES+=("$name"); RESULTS+=("PASS"); LOGS+=("$log")
  else
    local code=$?
    printf 'FAIL (exit %d)\n' "$code"
    NAMES+=("$name"); RESULTS+=("FAIL"); LOGS+=("$log")
  fi
}

skip_step() {
  local name="$1" reason="$2"
  printf '  %-42s SKIPPED: %s\n' "$name" "$reason"
  NAMES+=("$name"); RESULTS+=("SKIPPED"); LOGS+=("")
}

gradle_step() {
  ( cd "$ROOT/android" && ./gradlew "$@" )
}

echo "Health Trail verification"
echo "========================="
echo

echo "Content and contract checks"
run_step "compliance checks" python3 tools/checks/run_all.py
run_step "generated catalog matches its data" bash -c '
  python3 templates/build-catalog.py > /dev/null && git diff --quiet -- templates/CATALOG.md'

echo
echo "Android"
if [ ! -x "$ROOT/android/gradlew" ]; then
  skip_step "assemble debug" "no gradle wrapper"
  skip_step "unit tests" "no gradle wrapper"
  skip_step "instrumented suite compiles" "no gradle wrapper"
  skip_step "lint" "no gradle wrapper"
else
  run_step "assemble debug" gradle_step assembleDebug
  run_step "unit tests" gradle_step testDebugUnitTest
  run_step "instrumented suite compiles" gradle_step assembleDebugAndroidTest
  run_step "lint" gradle_step lintDebug
fi

echo
echo "Instrumented, on the connected phone"
if [ "$WITH_DEVICE" -eq 0 ]; then
  skip_step "instrumented suite runs" "not requested, pass --device"
elif [ ! -x "$ADB" ]; then
  skip_step "instrumented suite runs" "adb not found"
elif ! "$ADB" devices | grep -qE "device$"; then
  skip_step "instrumented suite runs" "no device attached"
else
  # **The shade is collapsed and the focus is checked before anything runs.**
  # A notification pulled down when the suite starts fails every Espresso test
  # that needs a focused root: six named back-journey tests, all red, in a
  # suite that was green an hour earlier, and the message blames the product.
  # It cost eight minutes on 2026-08-06 and muddied two real flakes. #316.
  #
  # **Refusing beats warning.** A warning in a seven minute log is a warning
  # nobody reads.
  "$ADB" shell cmd statusbar collapse >/dev/null 2>&1 || true
  "$ADB" shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
  sleep 1
  focus="$("$ADB" shell dumpsys window 2>/dev/null | grep -m1 'mCurrentFocus' || true)"
  case "$focus" in
    *Sys2040*|*NotificationShade*)
      skip_step "instrumented suite runs" "the notification shade is open: $focus"
      ;;
    *)
    # One operational step before this, and it is a checklist item rather than a
    # reason to avoid running: connectedAndroidTest uninstalls the application,
    # taking its data with it. If the phone holds anything worth keeping, export
    # through the app first and reimport afterward.
    echo "  note: connectedAndroidTest uninstalls the app. Export first if the phone holds data."
    run_step "instrumented suite runs" gradle_step connectedDebugAndroidTest

    # **The report is copied before anything can overwrite it.** A single class
    # rerun writes over `androidTest-results/connected/debug/TEST-*.xml`, and both
    # flakes this project has seen lost their assertion and stack exactly that
    # way: the next command destroyed the only evidence. #308 and #302.
    #
    # Kept in the build directory, which is already ignored, and named by when it
    # ran so two runs in one afternoon do not become one file.
    kept="$ROOT/android/app/build/outputs/androidTest-results/history"
    mkdir -p "$kept"
    stamp="$(date +%Y%m%d-%H%M%S)"
    for report in "$ROOT/android/app/build/outputs/androidTest-results/connected/debug/"TEST-*.xml; do
      [ -f "$report" ] || continue
      cp "$report" "$kept/$stamp-$(basename "$report")"
      echo "  report kept: ${kept#"$ROOT"/}/$stamp-$(basename "$report")"
    done
      ;;
  esac
fi

echo
echo "Test counts, read from the reports rather than from the log"
python3 - <<'COUNTS'
import glob, re
total = failed = 0
found = False
for path in sorted(glob.glob("android/app/build/test-results/**/*.xml", recursive=True)):
    text = open(path, encoding="utf-8", errors="replace").read()
    match = re.search(r'tests="(\d+)".*?failures="(\d+)".*?errors="(\d+)"', text)
    if not match:
        continue
    found = True
    name = re.search(r'name="([^"]+)"', text)
    label = name.group(1).split(".")[-1] if name else path
    bad = int(match.group(2)) + int(match.group(3))
    total += int(match.group(1))
    failed += bad
    print(f"  {label:34} {match.group(1):>4} run, {bad} failed")
if not found:
    print("  no test reports found, which means no suite ran")
else:
    print(f"  {'total':34} {total:>4} run, {failed} failed")
COUNTS

echo
echo "========================="
exit_code=0
failed_names=()
skipped_names=()
for index in "${!NAMES[@]}"; do
  case "${RESULTS[$index]}" in
    FAIL) failed_names+=("${NAMES[$index]}"); exit_code=1 ;;
    SKIPPED) skipped_names+=("${NAMES[$index]}") ;;
  esac
done

if [ ${#failed_names[@]} -gt 0 ]; then
  echo "FAILED: ${#failed_names[@]} step(s)"
  for index in "${!NAMES[@]}"; do
    if [ "${RESULTS[$index]}" = "FAIL" ]; then
      echo
      echo "--- ${NAMES[$index]} ---"
      grep -E "^e: |error:|Error:|FAILED|What went wrong" -A3 "${LOGS[$index]}" | head -25 \
        || tail -20 "${LOGS[$index]}"
    fi
  done
else
  echo "All executed steps passed."
fi

if [ ${#skipped_names[@]} -gt 0 ]; then
  echo
  echo "Skipped, which is not the same as passed:"
  printf '  %s\n' "${skipped_names[@]}"
fi

echo
echo "Full logs: $LOG_DIR"
exit "$exit_code"
