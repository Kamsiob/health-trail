#!/usr/bin/env python3
"""Retired. D178.

**This check reported zero and it was read as the overhaul being finished.**
It measured four mechanical tells, press state, corner radius, typeface and
Material imports, across the component and screen files. All four went to zero
while the interface was still, in the owner's words, sixty percent the old
design: old headers, old page titles, old accordions, old buttons, old
projects. It was silent on every one of those because none of them is a tell it
was given.

It is kept as a file rather than deleted so that the next session finds this
note instead of the command, and does not build another one. **A conformance
command measures what it was told to measure and says nothing about whether a
screen carries a design.** The thing it cannot check is the thing that was
wrong.

The interface is now being replaced rather than converted, so there is nothing
left for it to count. `docs/V4.md` is the plan; the phone is the check.
"""

import sys


def main() -> int:
    print(__doc__.strip())
    return 0


if __name__ == "__main__":
    sys.exit(main())
