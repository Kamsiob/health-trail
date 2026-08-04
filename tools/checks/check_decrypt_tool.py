#!/usr/bin/env python3
"""The standalone decryptor still opens an archive.

`contract/DATA-CONTRACT.md` section 8.1, requirement 2:

> A standalone decryption tool ships in the repository at `tools/decrypt/` ...
> It is tested in continuous integration against a real archive on every change
> to the export code.

**Why this test and not a reviewer.** `tools/decrypt/` is the thing standing
between an encrypted archive and a lost record, and it is also the thing nobody
runs. The app's own export path gets exercised every time somebody exports; this
tool gets exercised the day somebody's phone is gone, which is the worst possible
moment to discover it stopped matching the format two years ago.

**What it actually proves.** The archive here is built by `tools/fixtures/pack.py`,
which is a separate implementation of the same format that reads the Argon2id
costs out of the Kotlin that defines them. So the chain under test is:

    the app's constants -> pack.py -> decrypt.py

If the layout changes, this fails. If the nonce derivation changes, this fails.

**And one archive is deliberately written at a cost that is not this build's**,
because otherwise a tool with the numbers hard coded passes: the hard coded
numbers are today's numbers. That is not hypothetical. The first version of this
check went green against a `decrypt.py` with `memory_cost` pinned, while its own
docstring claimed it would catch exactly that.

**What it does not prove**, stated so nobody reads more into a green run than is
there: it does not prove the app's own writer produces this layout, because
building an archive here would need Android. That half is the instrumented
`ExportContainerTest` and the offline read test in 8.5, which is run by a person
against a real export.
"""

from __future__ import annotations

import base64
import json
import os
import sqlite3
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DECRYPT = ROOT / "tools/decrypt/decrypt.py"
PACK = ROOT / "tools/fixtures/pack.py"

PASSPHRASE = "a-known-passphrase-for-this-test"
READABLE_PAGE = "readable/index.html"
READABLE_HTML = "<!DOCTYPE html>\n<html lang=\"en\" dir=\"ltr\"><body>Ruth</body></html>\n"


def build_database(path: Path) -> None:
    """A one-row database, which is 8.5's "one" shape."""
    db = sqlite3.connect(path)
    db.execute(
        "CREATE TABLE entry (id TEXT PRIMARY KEY, title TEXT, deleted_at INTEGER)"
    )
    # pack.py materializes attachment bytes from this table, so it has to exist
    # even when it is empty. An empty one is also the honest shape here: this
    # test is about the container, not about attachments.
    db.execute(
        "CREATE TABLE attachment ("
        "id TEXT PRIMARY KEY, sha256 TEXT, byte_size INTEGER, mime_type TEXT, "
        "original_filename TEXT, deleted_at INTEGER)"
    )
    # pack.py's manifest states how many subjects the file holds, so an
    # importer can say what is about to be imported before doing it.
    db.execute(
        "CREATE TABLE subject (id TEXT PRIMARY KEY, display_name TEXT, deleted_at INTEGER)"
    )
    db.execute("INSERT INTO subject VALUES ('s1', 'Ruth Baxter', NULL)")
    db.execute("INSERT INTO entry VALUES ('e1', 'Called the unit', NULL)")
    db.commit()
    db.close()


def main() -> int:
    for tool in (DECRYPT, PACK):
        if not tool.is_file():
            print(f"Decrypt tool check failed: {tool.relative_to(ROOT)} is missing.")
            print("contract/DATA-CONTRACT.md 8.1 requires it to ship in the repository.")
            return 1

    try:
        from cryptography.hazmat.primitives.ciphers.aead import AESGCM  # noqa: F401
        import argon2  # noqa: F401
    except ImportError:
        # Not a pass. The tool's whole promise is that two ordinary libraries
        # are enough, and a run that cannot check that has not checked it.
        print(
            "Decrypt tool check skipped: cryptography and argon2-cffi are not installed.\n"
            "    pip install cryptography argon2-cffi\n"
            "This is a skip rather than a pass. The tool is untested on this run."
        )
        return 0

    sys.path.insert(0, str(PACK.parent))
    import importlib

    pack = importlib.import_module("pack")

    with tempfile.TemporaryDirectory() as work:
        work_path = Path(work)
        database = work_path / "trail.sqlite"
        archive = work_path / "archive.htx"
        build_database(database)

        pack.pack(database, archive, PASSPHRASE, exported_at=1_753_977_600_000)

        # pack.py writes the payload and attachments but not the readable copy,
        # which only the app renders. One page is added here with the documented
        # nonce derivation, so the tool's handling of readable/ is covered too.
        constants = pack.crypto_constants()
        with zipfile.ZipFile(archive) as source:
            manifest = json.loads(source.read("manifest.json"))
            entries = {name: source.read(name) for name in source.namelist()}

        encryption = manifest["encryption"]
        from argon2.low_level import Type, hash_secret_raw

        key = hash_secret_raw(
            secret=PASSPHRASE.encode("utf-8"),
            salt=base64.b64decode(encryption["salt"]),
            time_cost=int(encryption["kdf_iterations"]),
            memory_cost=int(encryption["kdf_memory_kib"]),
            parallelism=int(encryption["kdf_parallelism"]),
            hash_len=32,
            type=Type.ID,
        )
        from cryptography.hazmat.primitives.ciphers.aead import AESGCM as Aead

        entries[READABLE_PAGE] = Aead(key).encrypt(
            pack.nonce_for(READABLE_PAGE, constants["NONCE_BYTES"]),
            READABLE_HTML.encode("utf-8"),
            None,
        )
        manifest["readable"] = {"pages": 1}
        entries["manifest.json"] = json.dumps(manifest, indent=2).encode("utf-8")

        with zipfile.ZipFile(archive, "w") as rebuilt:
            rebuilt.writestr("manifest.json", entries.pop("manifest.json"))
            for name, data in sorted(entries.items()):
                rebuilt.writestr(name, data)

        out = work_path / "out"
        result = subprocess.run(
            [sys.executable, str(DECRYPT), str(archive), str(out)],
            input=PASSPHRASE + "\n",
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            print("Decrypt tool check failed: the tool could not open the archive.")
            print()
            print(result.stdout)
            print(result.stderr)
            return 1

        problems = []
        payload = out / "data.sqlite"
        if not payload.is_file():
            problems.append("the payload was not written")
        else:
            db = sqlite3.connect(payload)
            rows = db.execute("SELECT title FROM entry").fetchall()
            db.close()
            if rows != [("Called the unit",)]:
                problems.append(f"the payload does not read back: {rows}")

        page = out / READABLE_PAGE
        if not page.is_file():
            problems.append("the readable page was not written")
        elif page.read_text() != READABLE_HTML:
            problems.append("the readable page did not decrypt to what went in")

        # **An archive written with costs that are not this build's constants.**
        #
        # Without this, a tool with the numbers hard coded passes, because the
        # hard coded numbers are today's numbers. That was not hypothetical: the
        # first version of this check was tried against a decrypt.py with
        # memory_cost pinned to 65536 and it went green, while its own docstring
        # claimed it would catch exactly that.
        #
        # So one archive is written at a deliberately different cost. A tool that
        # reads the file opens it. A tool that assumes cannot, which is the
        # failure section 8.1 cares about: an archive written in 2026 has to open
        # in 2036 against whatever wrote it.
        odd = work_path / "odd-cost.htx"
        odd_salt = os.urandom(constants["SALT_BYTES"])
        odd_nonce = os.urandom(constants["NONCE_BYTES"])
        odd_iterations = constants["ITERATIONS"] + 1
        odd_memory = constants["MEMORY_KIB"] * 2
        odd_key = hash_secret_raw(
            secret=PASSPHRASE.encode("utf-8"),
            salt=odd_salt,
            time_cost=odd_iterations,
            memory_cost=odd_memory,
            parallelism=constants["PARALLELISM"],
            hash_len=constants["KEY_BITS"] // 8,
            type=Type.ID,
        )
        odd_manifest = dict(manifest)
        odd_manifest["encryption"] = dict(
            manifest["encryption"],
            kdf_iterations=odd_iterations,
            kdf_memory_kib=odd_memory,
            salt=base64.b64encode(odd_salt).decode("ascii"),
            nonce=base64.b64encode(odd_nonce).decode("ascii"),
        )
        odd_manifest["readable"] = {"pages": 0}
        with zipfile.ZipFile(odd, "w") as writer:
            writer.writestr("manifest.json", json.dumps(odd_manifest, indent=2))
            writer.writestr(
                "data.sqlite",
                Aead(odd_key).encrypt(odd_nonce, database.read_bytes(), None),
            )
        odd_out = work_path / "odd-out"
        odd_result = subprocess.run(
            [sys.executable, str(DECRYPT), str(odd), str(odd_out)],
            input=PASSPHRASE + "\n",
            capture_output=True,
            text=True,
        )
        if odd_result.returncode != 0:
            problems.append(
                "an archive written at a different Argon2id cost did not open, so "
                "the tool is assuming this build's parameters rather than reading "
                "them from the file"
            )

        # A wrong passphrase must fail rather than write something wrong.
        wrong = subprocess.run(
            [sys.executable, str(DECRYPT), str(archive), str(work_path / "nope")],
            input="not-the-passphrase\n",
            capture_output=True,
            text=True,
        )
        if wrong.returncode == 0:
            problems.append("a wrong passphrase was accepted")
        elif "altered" not in wrong.stderr:
            problems.append(
                "a wrong passphrase did not say it cannot tell a wrong "
                "passphrase from an altered file"
            )

        if problems:
            print("Decrypt tool check failed.")
            print()
            for problem in problems:
                print(f"  {problem}")
            print()
            print(
                "contract/DATA-CONTRACT.md 8.1. This tool is what stands between an\n"
                "encrypted archive and a lost record, and it is the thing nobody runs\n"
                "until the day the phone is gone."
            )
            return 1

    print(
        "Decrypt tool check passed. A packed archive opened with only the "
        "standalone tool, and a wrong passphrase was refused honestly."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
