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
the parameters recorded inside the file itself, decrypts the payload and every
attachment with AES-256-GCM, and writes the result into a folder you name.

Then open `readable/index.html` in any browser. That folder is the record in
plain HTML and needs no software at all.

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
import json
import os
import sys
import zipfile
from pathlib import Path

MANIFEST = "manifest.json"
PRIVATE_MANIFEST = "manifest-private.json"
README = "README.txt"
DATABASE = "data.sqlite"
ATTACHMENTS = "attachments/"
READABLE = "readable/"

SUPPORTED_FORMATS = (2,)


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
        # in the encrypted half of the manifest so that a backup agent, a cloud
        # sync, or a file manager preview learns nothing from the file sitting
        # in a folder. contract/DATA-CONTRACT.md 8.1.
        print()

        passphrase = getpass.getpass("Passphrase: ")
        print("Deriving the key. This is deliberately slow and takes a moment.")
        key = derive_key(manifest, passphrase, hash_secret_raw, Type)

        import base64

        nonce = base64.b64decode(manifest["encryption"]["nonce"])

        out.mkdir(parents=True, exist_ok=True)
        (out / MANIFEST).write_bytes(archive.read(MANIFEST))
        if README in archive.namelist():
            (out / README).write_bytes(archive.read(README))

        # The private half of the manifest, which is where the counts live.
        # Decrypted first because it is the cheapest thing in the archive: a
        # wrong passphrase fails here in milliseconds rather than after a large
        # payload has been through the cipher.
        if PRIVATE_MANIFEST in archive.namelist():
            private = json.loads(
                decrypt_entry(
                    AESGCM, key, attachment_nonce(PRIVATE_MANIFEST),
                    archive.read(PRIVATE_MANIFEST), "the archive's own description",
                ).decode("utf-8")
            )
            (out / PRIVATE_MANIFEST).write_text(json.dumps(private, indent=2))
            counts = private.get("database", {}).get("row_counts", {})
            if counts:
                total = sum(int(v) for v in counts.values())
                print(f"{total} records across {len(counts)} kinds")
            pages = private.get("readable", {}).get("pages")
            if pages:
                print(f"{pages} readable pages")
            print()

        payload = decrypt_entry(
            AESGCM, key, nonce, archive.read(DATABASE), "the record"
        )
        (out / DATABASE).write_bytes(payload)
        print(f"Wrote {DATABASE} ({len(payload):,} bytes)")

        pages = 0
        files = 0
        for name in sorted(archive.namelist()):
            if name in (MANIFEST, DATABASE, PRIVATE_MANIFEST, README) or name.endswith("/"):
                continue
            data = archive.read(name)
            if name.startswith(ATTACHMENTS):
                base = name[len(ATTACHMENTS):]
                data = decrypt_entry(
                    AESGCM, key, attachment_nonce(base), data, f"attachment {base}"
                )
                files += 1
            elif name.startswith(READABLE):
                # Encrypted like the payload and the attachments. A readable page
                # is the person's record in prose and needs no tooling to read,
                # so it is if anything more sensitive than the database.
                data = decrypt_entry(AESGCM, key, attachment_nonce(name), data, name)
                pages += 1
            destination = out / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(data)

        print(f"Wrote {files} attachments and {pages} readable pages")

    print()
    print(f"Done. Open this in any browser, with no internet connection:")
    print(f"    {(out / READABLE / 'index.html').resolve()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
