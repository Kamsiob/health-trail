#!/usr/bin/env python3
"""The build still meets what Google Play requires of an update.

**Play's target API requirement moves every year and refuses the upload rather
than warning about it.** Missing it does not degrade anything; it stops the
release. So the number belongs in a check next to its deadline and its source,
rather than in a comment somebody reads a year late.

**Verified on 2026-08-27 against the official pages, not against a summary.**

- `developer.android.com/google/play/requirements/target-sdk`, stating "Last
  updated 2026-08-14": **API 36 (Android 16) from 31 August 2026**, for new apps
  and for updates alike. An extension to 1 November 2026 can be requested.
- **The API 35 figure that appears in summaries is a different rule** and is not
  this one: it is the visibility threshold for an app that is never updated,
  which stops being discoverable to new users on newer devices. The two are
  routinely conflated.
- **Play does not check `compileSdk`.** It is a build concern. This file checks
  it only against `targetSdk`, because compiling below what you target is a
  configuration that cannot be built.

**What this check cannot see, and where that half lives.** Every app targeting
API 35 or higher must support 16 KB memory page sizes on 64-bit devices, and
from **1 February 2027** an update that does not will be refused
(`developer.android.com/guide/practices/page-sizes`, "Last updated 2026-08-23").
This app ships `libsqlcipher.so` and `libandroidx.graphics.path.so` across four
ABIs, so it is in scope. Alignment is a property of the built `.so` files and
cannot be read out of Gradle configuration, so it is measured on the bundle at
release time:

    readelf -lW base/lib/*/*.so | awk '/LOAD/{print $NF}' | sort -u

Every segment must report `0x4000`. The 1.0 bundle did, on all eight files.

Usage: python3 tools/checks/check_play_requirements.py

Kamsiob, AGPL-3.0.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BUILD = ROOT / "android" / "app" / "build.gradle.kts"

# The level Play requires of an update, and the date it started requiring it.
REQUIRED_TARGET_SDK = 36
REQUIRED_FROM = "31 August 2026"

# Android 8.0. Chosen for the audience rather than for convenience, D15: these
# are frequently older, cheaper, hand-me-down phones. A raise is a decision
# about who can no longer install the app, so it fails here rather than passing
# quietly.
EXPECTED_MIN_SDK = 26


def level(body: str, name: str) -> int | None:
    found = re.search(rf"^\s*{name}\s*=\s*(\d+)\s*$", body, re.M)
    return int(found.group(1)) if found else None


def main() -> int:
    body = BUILD.read_text(encoding="utf-8")
    problems = []

    compile_sdk = level(body, "compileSdk")
    target_sdk = level(body, "targetSdk")
    min_sdk = level(body, "minSdk")

    for name, value in (("compileSdk", compile_sdk), ("targetSdk", target_sdk), ("minSdk", min_sdk)):
        if value is None:
            problems.append(
                f"{name} could not be read out of {BUILD.relative_to(ROOT)}. "
                "Either it moved or its form changed, and this check is now blind."
            )

    if target_sdk is not None and target_sdk < REQUIRED_TARGET_SDK:
        problems.append(
            f"targetSdk is {target_sdk}. Google Play has required "
            f"{REQUIRED_TARGET_SDK} of every update since {REQUIRED_FROM}, and "
            "refuses the upload rather than warning. See DECISIONS.md D15."
        )

    if compile_sdk is not None and target_sdk is not None and compile_sdk < target_sdk:
        problems.append(
            f"compileSdk {compile_sdk} is below targetSdk {target_sdk}, which "
            "cannot build: an app cannot opt into behavior from a platform it "
            "was not compiled against."
        )

    if min_sdk is not None and min_sdk != EXPECTED_MIN_SDK:
        problems.append(
            f"minSdk is {min_sdk}, not {EXPECTED_MIN_SDK}. Raising it decides "
            "who can no longer install the app, which D15 settled on the "
            "audience rather than on convenience. Change it there first."
        )

    if problems:
        print("Play requirements check failed.")
        for problem in problems:
            print(f"  {problem}")
        return 1

    print(
        f"Play requirements check passed. compileSdk {compile_sdk}, "
        f"targetSdk {target_sdk} against the {REQUIRED_TARGET_SDK} required "
        f"since {REQUIRED_FROM}, minSdk {min_sdk}."
    )
    print("  16 KB page alignment is a property of the built .so files and is")
    print("  measured on the bundle at release time. See this file's docstring.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
