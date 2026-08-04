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
    # **Large enough to be several frames.** The truncation test below drops the
    # final frame, and a payload of one frame has no final frame to drop: cutting
    # it leaves nothing, and the check then passes on the wrong reason. The first
    # version of this file did exactly that, and a probe with the tool's
    # final-frame check removed still went green.
    db.execute("CREATE TABLE bulk (id INTEGER PRIMARY KEY, body BLOB)")
    filler = bytes((i * 7) % 256 for i in range(65536))
    for row in range(48):
        db.execute("INSERT INTO bulk VALUES (?, ?)", (row, filler))
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

        problems = []

        # **The outer layer holds exactly three things**, which is 8.1's word.
        # Checked here as well as in the app's own tests because this is the
        # side a stranger sees, and the fixture packer is a second
        # implementation that could drift from the app without anything saying
        # so until somebody's phone was gone.
        with zipfile.ZipFile(archive) as source:
            outer = source.namelist()
            manifest = json.loads(source.read("MANIFEST.json"))
        if sorted(outer) != sorted(["MANIFEST.json", "README.txt", "payload.enc"]):
            problems.append(f"the outer layer holds {outer}, not the three 8.1 names")
        for leak in ("row_counts", "origin_device", "subject_count"):
            if leak in json.dumps(manifest):
                problems.append(f"the outer manifest leaks {leak}")

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

        payload = out / "data" / "trail.sqlite"
        if not payload.is_file():
            problems.append("the payload was not written")
        else:
            db = sqlite3.connect(payload)
            rows = db.execute("SELECT title FROM entry").fetchall()
            db.close()
            if rows != [("Called the unit",)]:
                problems.append(f"the payload does not read back: {rows}")

        for required in ("README.txt", "MANIFEST.json", "CHECKSUMS.txt", "data/schema.sql"):
            if not (out / required).is_file():
                problems.append(f"the inner container has no {required}")

        # **A cut payload and a rearranged one both have to be refused.** Both
        # of these are real assertions and both pass. What follows is which layer
        # actually earns each, because the first two versions of this comment
        # claimed credit for the wrong one and a probe proved it.
        #
        # A tail cut is caught by the zip inside the payload, whose central
        # directory lives at the end. Removing the tool's final-frame check and
        # rerunning this still went green, twice, because the truncated inner zip
        # simply failed to parse.
        #
        # A reordering is caught by the nonce, not by the authenticated data:
        # frame N's nonce is the prefix followed by N, so a frame moved to
        # another position is decrypted under the wrong nonce and fails. Unbinding
        # the index from the additional data on both sides and rerunning did not
        # let a swapped archive through either.
        #
        # **So the index and the final flag in the additional data are belt and
        # braces, and this file says so rather than implying they are the only
        # thing standing between somebody and a silently shortened record.** They
        # are worth keeping: they are free, and they hold if a later version ever
        # puts something other than a zip inside the payload, at which point the
        # structural protection this leans on today disappears.
        cut = work_path / "cut.htx"
        with zipfile.ZipFile(archive) as source:
            entries = {name: source.read(name) for name in source.namelist()}
        frames = frame_bounds(entries["payload.enc"])
        if len(frames) < 3:
            problems.append(
                f"the test payload is {len(frames)} frame(s), so dropping the last one "
                "does not test what this claims: make the fixture larger"
            )
        with zipfile.ZipFile(cut, "w") as rebuilt:
            for name, data in entries.items():
                if name == "payload.enc":
                    # **Cut on a frame boundary, not at an arbitrary byte.** A
                    # cut in the middle of a frame is caught by the length
                    # prefix, which proves nothing about the final flag. Dropping
                    # whole frames leaves a file every remaining frame of which
                    # verifies perfectly, and only the missing final flag says
                    # anything is wrong.
                    data = data[: frames[-1]]
                rebuilt.writestr(name, data)
        short = subprocess.run(
            [sys.executable, str(DECRYPT), str(cut), str(work_path / "cut-out")],
            input=PASSPHRASE + "\n",
            capture_output=True,
            text=True,
        )
        if short.returncode == 0:
            problems.append(
                "a payload with its tail cut off was opened rather than refused, so a "
                "truncated archive would hand somebody a partial record and call it whole"
            )

        # **Two frames swapped.** The file is the same length and every frame is
        # a genuine frame written by the real writer under the real key. It must
        # not open. See the note above for which mechanism refuses it.
        swapped = work_path / "swapped.htx"
        payload_bytes = entries["payload.enc"]
        first, second, third = frames[0], frames[1], frames[2]
        reordered = (
            payload_bytes[:first]
            + payload_bytes[second:third]
            + payload_bytes[first:second]
            + payload_bytes[third:]
        )
        with zipfile.ZipFile(swapped, "w") as rebuilt:
            for name, data in entries.items():
                rebuilt.writestr(name, reordered if name == "payload.enc" else data)
        moved = subprocess.run(
            [sys.executable, str(DECRYPT), str(swapped), str(work_path / "swapped-out")],
            input=PASSPHRASE + "\n",
            capture_output=True,
            text=True,
        )
        if moved.returncode == 0:
            problems.append(
                "a payload with two frames swapped was opened, so frames are not bound "
                "to their positions and an archive can be silently rearranged"
            )

        # **An archive written at a cost that is not this build's.**
        #
        # Without this, a tool with the numbers hard coded passes, because the
        # hard coded numbers are today's numbers. That was not hypothetical: the
        # first version of this check was tried against a decrypt.py with
        # memory_cost pinned to 65536 and it went green, while its own docstring
        # claimed it would catch exactly that.
        odd = work_path / "odd-cost.htx"
        rewrite_costs(pack, archive, odd)
        odd_result = subprocess.run(
            [sys.executable, str(DECRYPT), str(odd), str(work_path / "odd-out")],
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
        "standalone tool, a truncated one was refused, and a wrong passphrase "
        "was refused honestly."
    )
    return 0


def rewrite_costs(pack, archive: Path, target: Path) -> None:
    """The same archive, resealed at an Argon2id cost this build does not use.

    A tool that reads the parameters out of the file opens it. A tool that
    assumes today's numbers cannot, which is the failure 8.1 cares about: an
    archive written in 2026 has to open in 2036 against whatever wrote it.
    """
    from argon2.low_level import Type, hash_secret_raw
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM

    constants = pack.crypto_constants()
    with zipfile.ZipFile(archive) as source:
        manifest = json.loads(source.read("MANIFEST.json"))
        sealed = source.read("payload.enc")
        readme = source.read("README.txt")

    encryption = manifest["encryption"]
    old_key = hash_secret_raw(
        secret=PASSPHRASE.encode("utf-8"),
        salt=base64.b64decode(encryption["salt"]),
        time_cost=int(encryption["kdf_iterations"]),
        memory_cost=int(encryption["kdf_memory_kib"]),
        parallelism=int(encryption["kdf_parallelism"]),
        hash_len=32,
        type=Type.ID,
    )
    prefix = base64.b64decode(encryption["nonce_prefix"])
    plain = unseal(AESGCM(old_key), prefix, sealed)

    salt = os.urandom(constants["SALT_BYTES"])
    new_prefix = os.urandom(constants["NONCE_PREFIX_BYTES"])
    iterations = constants["ITERATIONS"] + 1
    memory = constants["MEMORY_KIB"] * 2
    new_key = hash_secret_raw(
        secret=PASSPHRASE.encode("utf-8"),
        salt=salt,
        time_cost=iterations,
        memory_cost=memory,
        parallelism=constants["PARALLELISM"],
        hash_len=constants["KEY_BITS"] // 8,
        type=Type.ID,
    )
    manifest["encryption"] = dict(
        encryption,
        kdf_iterations=iterations,
        kdf_memory_kib=memory,
        salt=base64.b64encode(salt).decode("ascii"),
        nonce_prefix=base64.b64encode(new_prefix).decode("ascii"),
    )
    with zipfile.ZipFile(target, "w") as rebuilt:
        rebuilt.writestr("MANIFEST.json", json.dumps(manifest, indent=2))
        rebuilt.writestr("README.txt", readme)
        rebuilt.writestr(
            "payload.enc",
            pack.seal(AESGCM(new_key), new_prefix, plain, constants["CHUNK_BYTES"]),
        )


def frame_bounds(sealed: bytes) -> list:
    """Where each frame starts, so a test can cut exactly between two."""
    starts = []
    at = 0
    while at < len(sealed):
        starts.append(at)
        size = int.from_bytes(sealed[at:at + 4], "big")
        at += 4 + size
    return starts


def unseal(aead, prefix: bytes, sealed: bytes) -> bytes:
    """The frames, back into the inner container. The reader's half of pack.seal."""
    out = bytearray()
    at = 0
    index = 0
    while at < len(sealed):
        size = int.from_bytes(sealed[at:at + 4], "big")
        at += 4
        frame = sealed[at:at + size]
        at += size
        last = at >= len(sealed)
        nonce = prefix + index.to_bytes(8, "big")
        out += aead.decrypt(nonce, frame, index.to_bytes(8, "big") + (b"\x01" if last else b"\x00"))
        index += 1
    return bytes(out)


if __name__ == "__main__":
    sys.exit(main())
