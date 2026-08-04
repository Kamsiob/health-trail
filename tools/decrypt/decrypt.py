#!/usr/bin/env python3
"""Open a Health Trail archive without Health Trail.

    python3 decrypt.py health-trail-2027-03-14.zip out/

This is not a convenience. It is one of the three requirements in
`contract/DATA-CONTRACT.md` section 8.1 that make an encrypted archive
**openable by somebody who has the passphrase and does not have this app**.

A format only one program can decrypt is the same failure as a format only one
program can read, arriving one step later. The app that wrote your archive may
be gone. The phone certainly will be. This file, and the specification in
`contract/EXPORT-FORMAT.md` it was written from, are what stand between an
encrypted archive and a lost record.

WHAT YOU NEED

Python 3.9 or newer, and two libraries:

    pip install cryptography argon2-cffi

Nothing else. No build step, no Health Trail, no internet connection once the
libraries are installed.

WHAT IT DOES

Reads the archive, asks for your passphrase, derives the key with Argon2id using
the parameters recorded inside the file itself, decrypts `payload.enc` with
AES-256-GCM, and unpacks what comes out into a folder you name.

The archive has two layers on purpose. The outer one is a plain zip holding
three things and saying nothing about anybody: a README, a manifest with the
key derivation parameters, and the encrypted payload. The inner one, once
decrypted, is an ordinary zip with the whole record in it.

Then open `readable/index.html` in any browser. That folder is the record in
plain HTML and needs no software at all.

WHY THE PAYLOAD IS IN FRAMES

An archive can be gigabytes, and encrypting it as one block would mean holding
all of it in memory at both ends. So `payload.enc` is a run of frames: four
bytes of big-endian length, then that many bytes of ciphertext with its tag.

Frame N uses a nonce of the manifest's four byte prefix followed by N as eight
big-endian bytes, and authenticates those same eight bytes plus one more that is
1 on the last frame and 0 on every other. **That last byte is why a truncated
file fails instead of opening short.** Without it every frame still verifies on
its own, so a file with its tail cut off would decrypt perfectly and be missing
a year of somebody's record, silently.

WHY THE PARAMETERS COME FROM THE FILE

Hardware gets faster and the recommended cost of a key derivation goes up with
it. An archive written in 2026 must still open in 2036 against whatever it was
written with, so the salt, the nonce and the three Argon2id costs travel in the
archive rather than being assumed here. A tool that assumed today's numbers
would fail to derive the right key from a **correct** passphrase and would
report a wrong passphrase, which tells somebody their memory is wrong when their
file is fine. That is the worst available failure for somebody's only copy.

A NOTE ON WHAT A FAILURE MEANS

AES-GCM authenticates as well as encrypts, so a wrong passphrase and an altered
file produce the same error: the tag did not verify. This tool says so and does
not guess which, because telling somebody their file is corrupt when they
mistyped is as bad as the reverse.
"""

from __future__ import annotations

import getpass
import io
import json
import os
import sys
import zipfile
from pathlib import Path

MANIFEST = "MANIFEST.json"
README = "README.txt"
PAYLOAD = "payload.enc"

INNER_MANIFEST = "MANIFEST.json"
CHECKSUMS = "CHECKSUMS.txt"
DATABASE = "data/trail.sqlite"
READABLE = "readable/"

SUPPORTED_FORMATS = (3,)

# Four bytes of big-endian length ahead of each frame's ciphertext.
FRAME_HEADER_BYTES = 4

# A length prefix is an instruction from whoever wrote the file. One that says
# four gigabytes is how a reader is made to exhaust memory before it has
# authenticated anything.
MAX_FRAME_BYTES = 64 << 20


def fail(message: str) -> None:
    print(f"\n{message}\n", file=sys.stderr)
    raise SystemExit(1)


def load_libraries():
    try:
        from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    except ImportError:
        fail(
            "This needs the 'cryptography' library.\n\n"
            "    pip install cryptography argon2-cffi"
        )
    try:
        from argon2.low_level import Type, hash_secret_raw
    except ImportError:
        fail(
            "This needs the 'argon2-cffi' library.\n\n"
            "    pip install cryptography argon2-cffi"
        )
    return AESGCM, hash_secret_raw, Type


def read_manifest(archive: zipfile.ZipFile) -> dict:
    try:
        raw = archive.read(MANIFEST)
    except KeyError:
        fail(
            f"This file has no {MANIFEST}, so it is not a Health Trail archive,\n"
            "or it has been altered since it was made."
        )
    try:
        manifest = json.loads(raw)
    except json.JSONDecodeError:
        fail(f"{MANIFEST} is not readable. The file has been damaged or altered.")

    version = manifest.get("format_version")
    if version not in SUPPORTED_FORMATS:
        fail(
            f"This archive says it is format version {version}.\n"
            f"This tool understands {', '.join(str(v) for v in SUPPORTED_FORMATS)}.\n\n"
            "A newer version of this tool, or of the specification in\n"
            "contract/EXPORT-FORMAT.md, will say how to read it. Nothing was changed."
        )
    return manifest


def derive_key(manifest: dict, passphrase: str, hash_secret_raw, Type) -> bytes:
    encryption = manifest.get("encryption")
    if not encryption:
        fail(
            "This archive says it is not encrypted, so there is nothing to decrypt.\n"
            "Open it with any zip tool."
        )

    kdf = encryption.get("kdf")
    if kdf != "Argon2id":
        fail(f"This archive was written with an unfamiliar key derivation: {kdf}.")

    import base64

    salt = base64.b64decode(encryption["salt"])
    # From the file, never from a constant here. See the module docstring.
    return hash_secret_raw(
        secret=passphrase.encode("utf-8"),
        salt=salt,
        time_cost=int(encryption["kdf_iterations"]),
        memory_cost=int(encryption["kdf_memory_kib"]),
        parallelism=int(encryption["kdf_parallelism"]),
        hash_len=32,
        type=Type.ID,
    )


def decrypt_entry(AESGCM, key: bytes, nonce: bytes, data: bytes, what: str) -> bytes:
    try:
        return AESGCM(key).decrypt(nonce, data, None)
    except Exception:
        fail(
            f"Could not decrypt {what}.\n\n"
            "Either the passphrase is not the one this archive was made with,\n"
            "or the file has been altered since it was made. There is no way to\n"
            "tell which from here: the check that failed cannot distinguish them.\n\n"
            "If you are sure of the passphrase, try a different copy of the file."
        )


def attachment_nonce(name: str) -> bytes:
    """The nonce for one attachment or one readable page.

    Derived from the entry's own name inside the archive, which is unique there,
    so every entry gets a distinct nonce under the same key without the archive
    having to carry one per file. `contract/EXPORT-FORMAT.md` specifies this and
    it is reimplemented here from that document rather than from the app's
    source, which is the point of the exercise.

    Attachments are named by content hash, so the name is the hash; readable
    pages use their full path inside the archive.
    """
    import hashlib

    return hashlib.sha256(name.encode("utf-8")).digest()[:12]


def unseal(AESGCM, key: bytes, prefix: bytes, sealed: bytes) -> bytes:
    """The payload's frames, back into the inner container."""
    aead = AESGCM(key)
    out = bytearray()
    at = 0
    index = 0
    finished = False
    while at < len(sealed):
        if finished:
            fail("This archive continues past the frame that says it ends.")
        if at + FRAME_HEADER_BYTES > len(sealed):
            fail("This archive ends in the middle of a frame header.")
        size = int.from_bytes(sealed[at:at + FRAME_HEADER_BYTES], "big")
        at += FRAME_HEADER_BYTES
        if not 0 < size <= MAX_FRAME_BYTES or at + size > len(sealed):
            fail("This archive declares a frame that is not there. It is damaged.")
        frame = sealed[at:at + size]
        at += size
        nonce = prefix + index.to_bytes(8, "big")
        try:
            out += aead.decrypt(nonce, frame, index.to_bytes(8, "big") + b"\x00")
        except Exception:
            try:
                out += aead.decrypt(nonce, frame, index.to_bytes(8, "big") + b"\x01")
                finished = True
            except Exception:
                fail(
                    "Could not decrypt the record.\n\n"
                    "Either the passphrase is not the one this archive was made with,\n"
                    "or the file has been altered since it was made. There is no way to\n"
                    "tell which from here: the check that failed cannot distinguish them.\n\n"
                    "If you are sure of the passphrase, try a different copy of the file."
                )
        index += 1
    if not finished:
        fail(
            "This archive has no final frame, so it was cut short somewhere.\n"
            "Some of it may be readable, but this tool will not hand you a partial\n"
            "record and call it whole. Try another copy of the file."
        )
    return bytes(out)


def check_contents(out: Path) -> None:
    """Every file against CHECKSUMS.txt, and say plainly what does not match."""
    listing = out / CHECKSUMS
    if not listing.is_file():
        return
    import hashlib

    bad = []
    for line in listing.read_text().splitlines():
        parts = line.split("  ", 1)
        if len(parts) != 2:
            continue
        expected, name = parts
        target = out / name
        if not target.is_file():
            bad.append(f"{name} is missing")
            continue
        got = hashlib.sha256(target.read_bytes()).hexdigest()
        if got != expected:
            bad.append(f"{name} does not match its checksum")
    if bad:
        print()
        print("Some files do not match the archive's own list of contents:")
        for problem in bad:
            print(f"  {problem}")
        print()
        print("They were still written out, so you can look at them. The rest of")
        print("the archive is unaffected.")


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        print("Usage: python3 decrypt.py <archive> <output folder>")
        return 2

    archive_path = Path(sys.argv[1])
    out = Path(sys.argv[2])
    if not archive_path.is_file():
        fail(f"There is no file at {archive_path}.")

    AESGCM, hash_secret_raw, Type = load_libraries()

    with zipfile.ZipFile(archive_path) as archive:
        manifest = read_manifest(archive)

        print(f"Health Trail archive, format version {manifest['format_version']}")
        written = manifest.get("app_version")
        if written:
            print(f"Written by Health Trail {written}")
        # Nothing about the person is readable yet, by design. The counts live
        # inside the payload so that a backup agent, a cloud sync, or a file
        # manager preview learns nothing from the file sitting in a folder.
        # contract/DATA-CONTRACT.md 8.1.
        print()

        passphrase = getpass.getpass("Passphrase: ")
        print("Deriving the key. This is deliberately slow and takes a moment.")
        key = derive_key(manifest, passphrase, hash_secret_raw, Type)

        import base64

        prefix = base64.b64decode(manifest["encryption"]["nonce_prefix"])

        if PAYLOAD not in archive.namelist():
            fail(
                f"This archive has no {PAYLOAD}, so there is nothing in it to open.\n"
                "It may have been written by a different program."
            )

        print("Decrypting.")
        inner = unseal(AESGCM, key, prefix, archive.read(PAYLOAD))

    out.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(io.BytesIO(inner)) as payload:
        payload.extractall(out)
        names = [n for n in payload.namelist() if not n.endswith("/")]

    pages = sum(1 for n in names if n.startswith(READABLE))
    files = sum(1 for n in names if n.startswith("attachments/"))
    print(f"Wrote {len(names)} files: {pages} readable pages and {files} attachments")

    check_contents(out)

    print()
    print("Done. Open this in any browser, with no internet connection:")
    print(f"    {(out / READABLE / 'index.html').resolve()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
