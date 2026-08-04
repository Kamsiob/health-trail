#!/usr/bin/env python3
"""The published format specification still describes what the code writes.

`contract/EXPORT-FORMAT.md` is one of the three things `contract/DATA-CONTRACT.md`
section 8.1 says makes an encrypted archive openable by somebody who does not
have this app. It is only that if it is **true**.

**A specification that has drifted is worse than none**, and worse in a specific
way: somebody in 2036 with an archive, the passphrase, and this document will
write a reader from it, and every number they take from it will be one they
cannot check against anything. A wrong frame size or a wrong nonce rule sends
them to conclude the file is damaged.

**What this checks and what it cannot.** It checks that every constant the
document states matches the constant the code uses, and that the document still
names the layout the writer writes. It cannot check that the prose is right about
why, and it cannot check that the document is complete. Those are read by a
person. What it removes is the failure that needs nobody to be careless: a number
changed in one file and not the other.

The companion check is `check_decrypt_tool.py`, which proves a tool written from
this document opens a real archive. Between them: the tool works, and the
document says what the tool does.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = ROOT / "contract/EXPORT-FORMAT.md"
CRYPTO = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/data/ExportCrypto.kt"
CONTAINER = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/data/ExportContainer.kt"


def kotlin_const(text: str, name: str) -> int:
    """A `const val NAME = ...` from Kotlin, including `1 shl 20`."""
    match = re.search(rf"const val {name}\s*=\s*([^\n/]+)", text)
    if not match:
        raise SystemExit(f"could not find {name}")
    value = match.group(1).strip()
    if "shl" in value:
        left, right = value.split("shl")
        return int(left.strip()) << int(right.strip())
    return int(value)


def kotlin_string(text: str, name: str) -> str:
    match = re.search(rf'const val {name}\s*=\s*"([^"]*)"', text)
    if not match:
        raise SystemExit(f"could not find {name}")
    return match.group(1)


def main() -> int:
    spec = SPEC.read_text(encoding="utf-8")
    crypto = CRYPTO.read_text(encoding="utf-8")
    container = CONTAINER.read_text(encoding="utf-8")

    problems: list[str] = []

    # -- the numbers, each of which a reimplementation would take on trust ----
    numbers = {
        "kdf_iterations": kotlin_const(crypto, "ITERATIONS"),
        "kdf_memory_kib": kotlin_const(crypto, "MEMORY_KIB"),
        "kdf_parallelism": kotlin_const(crypto, "PARALLELISM"),
        "chunk_bytes": kotlin_const(crypto, "CHUNK_BYTES"),
    }
    for field, value in numbers.items():
        # The specimen manifest in section 2 is what somebody reads first, so it
        # is the copy that has to be right rather than only the prose.
        if not re.search(rf'"{field}":\s*{value}\b', spec):
            problems.append(
                f"the specimen manifest does not say {field} is {value}, which is "
                f"what ExportCrypto writes"
            )

    salt_bytes = kotlin_const(crypto, "SALT_BYTES")
    if f"{salt_bytes} bytes from a CSPRNG" not in spec:
        problems.append(f"the salt is {salt_bytes} bytes and the spec does not say so")

    prefix_bytes = kotlin_const(crypto, "NONCE_PREFIX_BYTES")
    if f"**{prefix_bytes} bytes** from a CSPRNG" not in spec:
        problems.append(
            f"the nonce prefix is {prefix_bytes} bytes and the spec does not say so"
        )

    nonce_bytes = kotlin_const(crypto, "NONCE_BYTES")
    if f"is {nonce_bytes} bytes" not in spec:
        problems.append(f"the nonce is {nonce_bytes} bytes and the spec does not say so")

    tag_bits = kotlin_const(crypto, "TAG_BITS")
    if f"{tag_bits // 8} byte GCM tag" not in spec:
        problems.append(f"the tag is {tag_bits} bits and the spec does not say so")

    key_bits = kotlin_const(crypto, "KEY_BITS")
    if f"{key_bits} bit key" not in spec:
        problems.append(f"the key is {key_bits} bits and the spec does not say so")

    version = kotlin_const(container, "FORMAT_VERSION")
    if f'"format_version": {version}' not in spec:
        problems.append(f"the format is version {version} and the specimen manifest is not")
    if f"format version {version}" not in spec:
        problems.append(f"the spec's title does not name version {version}")

    # -- the layout, name by name, in the blocks that draw it ----------------
    #
    # **Against the layout blocks rather than the whole document.** Asking
    # whether a name appears anywhere in the file passes on a document that
    # mentions it in a sentence and draws something else in the diagram, which is
    # exactly the drift that matters: the diagram is what somebody copies. Probed
    # by renaming the database in the layout block and watching a whole-document
    # search stay green.
    #
    # **The language tag has to be optional in the pattern.** Without it the
    # match ran from one fence past the next, swallowing the prose between an
    # untagged block and a ```json one, so "the layout blocks" quietly meant
    # "most of the document" and the check named the wrong file when it fired.
    blocks = "\n".join(re.findall(r"```[a-z]*\n(.*?)```", spec, re.S))
    for const in (
        "OUTER_MANIFEST", "OUTER_README", "PAYLOAD",
        "INNER_MANIFEST", "INNER_README", "CHECKSUMS", "DATABASE", "SCHEMA",
        "ATTACHMENTS", "READABLE",
    ):
        name = kotlin_string(container, const)
        if name not in blocks:
            problems.append(
                f"the writer writes {name} and no layout block in the spec draws it"
            )

    # -- the frame header, which is the one piece of pure binary --------------
    header_bytes = kotlin_const(container, "FRAME_HEADER_BYTES")
    if f"{header_bytes} bytes, BE" not in spec:
        problems.append(
            f"the frame header is {header_bytes} bytes and the spec's diagram disagrees"
        )

    # -- the name rules, which a writer on another platform has to honor ------
    max_path = kotlin_const(container, "MAX_PATH_LENGTH")
    if f"longer than {max_path} characters" not in spec:
        problems.append(f"the path limit is {max_path} and the spec does not say so")

    if problems:
        print("Format specification check failed.")
        print()
        for problem in problems:
            print(f"  {problem}")
        print()
        print(
            "contract/EXPORT-FORMAT.md is what somebody writes a reader from in ten\n"
            "years, with no way to check any number in it against anything. A\n"
            "specification that has drifted from the code sends them to conclude\n"
            "their archive is damaged."
        )
        return 1

    print(
        "Format specification check passed. Every constant the specification "
        "states matches the code, and it names every entry the writer writes."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
