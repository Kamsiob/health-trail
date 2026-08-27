#!/usr/bin/env bash
# What a bundle has to prove before it is uploaded to Google Play.
#
# **Two of these cannot be read out of the source tree**, which is why they are
# here rather than in tools/checks: 16 KB page alignment is a property of the
# built .so files, and the signature is a property of the signed artifact.
#
# 16 KB page sizes: every app targeting API 35 or higher must support them on
# 64-bit devices, and from 1 February 2027 an update that does not will be
# refused. developer.android.com/guide/practices/page-sizes, "Last updated
# 2026-08-23". This app ships libsqlcipher.so and libandroidx.graphics.path.so
# across four ABIs, so it is in scope. DECISIONS.md D15.
#
# Usage: tools/store/check-bundle.sh <path to .aab>
#
# Kamsiob, AGPL-3.0.

set -euo pipefail

BUNDLE="${1:?usage: check-bundle.sh <path to .aab>}"
[ -f "$BUNDLE" ] || { echo "no bundle at $BUNDLE"; exit 1; }

command -v readelf >/dev/null || { echo "readelf is not installed"; exit 1; }
command -v unzip >/dev/null || { echo "unzip is not installed"; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -r -- "$WORK" 2>/dev/null || true' EXIT

echo "bundle:  $BUNDLE"
echo "bytes:   $(stat -c%s "$BUNDLE")"
echo "sha256:  $(sha256sum "$BUNDLE" | cut -d' ' -f1)"

unzip -o -q "$BUNDLE" 'base/lib/*' -d "$WORK" 2>/dev/null || true

FOUND=0
BAD=0
while IFS= read -r so; do
  FOUND=$((FOUND + 1))
  # Every LOAD segment has to be aligned to 16384 bytes, which readelf prints
  # as 0x4000. A single segment below it fails the whole library.
  aligns="$(readelf -lW "$so" | awk '/LOAD/ {print $NF}' | sort -u)"
  if [ "$aligns" != "0x4000" ]; then
    echo "  NOT 16 KB ALIGNED: ${so#"$WORK"/}  (LOAD alignment: $aligns)"
    BAD=$((BAD + 1))
  fi
done < <(find "$WORK" -name '*.so' | sort)

if [ "$FOUND" -eq 0 ]; then
  echo "  no native libraries in the bundle, so 16 KB alignment does not apply"
else
  if [ "$BAD" -ne 0 ]; then
    echo
    echo "$BAD of $FOUND native libraries are not 16 KB aligned."
    echo "Play refuses these updates from 1 February 2027. DECISIONS.md D15."
    exit 1
  fi
  echo "  $FOUND native libraries, every LOAD segment aligned to 0x4000 (16 KB)"
fi

if command -v keytool >/dev/null; then
  echo
  echo "signature:"
  keytool -printcert -jarfile "$BUNDLE" 2>/dev/null \
    | grep -E "Owner:|SHA256:" | sed 's/^/  /' | head -4
else
  echo "  keytool not installed, signature not read"
fi

echo
echo "Ready to upload."
