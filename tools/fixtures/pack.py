#!/usr/bin/env python3
"""Wrap a generated fixture in an export container the app can actually open.

**This is what makes the thirteen personas walkable on the real device.**

`generate.py` writes a plain SQLite file, which is the right artifact and which
the phone could not read: the app's database is SQLCipher keyed by 32 random
bytes wrapped in the phone's own Keystore, so there has never been a way to put
a five year notebook onto the device short of tapping it in by hand. Ten of the
thirteen personas need a horizon nobody can reach by tapping.

**The portability fix of 2026-08-02 is what opened this door**, and it is worth
saying why. Before it, the export carried the SQLCipher file as it sat on disk,
so no fixture could ever have been packed into one. D61 made the payload a
plain SQLite database precisely so a file could travel between machines. This
is that property being used in the other direction: a machine that is not a
phone writing a file the phone will accept.

**It goes in through the app's own restore screen.** Nothing here reaches into
the app's storage, nothing is pushed into its data directory, and no debug hook
exists to be left behind. The fixture arrives the way somebody else's backup
would, which means seeding a persona also exercises the import path every time.

`contract/EXPORT-FORMAT.md` is the specification. This writes format version 2,
which is encrypted only, per D67.

Usage:

    python3 tools/fixtures/generate.py --at year1 --seed 1 --out /tmp/y1.sqlite
    python3 tools/fixtures/pack.py --db /tmp/y1.sqlite --out /tmp/y1.htx \\
        --passphrase "a passphrase for the walk"

Then hand the file to the phone and open it from More, Restore from a file.

Kamsiob, AGPL-3.0.
"""

import argparse
import base64
import hashlib
import io
import json
import os
import sqlite3
import sys
import zipfile
from pathlib import Path

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.argon2 import Argon2id

# **Read from the app rather than chosen here.** These have to match
# `ExportCrypto`, and a second copy of a number is a second place for it to go
# stale. The manifest records them anyway, so the app reads what it is given
# rather than assuming, which is what lets the cost rise later without
# stranding an older file. D51.
ROOT = Path(__file__).resolve().parents[2]
CRYPTO = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/data/ExportCrypto.kt"

FORMAT_VERSION = 3
MANIFEST = "MANIFEST.json"
README = "README.txt"
PAYLOAD = "payload.enc"
INNER_MANIFEST = "MANIFEST.json"
CHECKSUMS = "CHECKSUMS.txt"
DATABASE = "data/trail.sqlite"
SCHEMA = "data/schema.sql"
ATTACHMENTS = "attachments/"

# The frame size the app writes. Read from the Kotlin below rather than pinned,
# for the same reason the Argon2 costs are.
FRAME_HEADER_BYTES = 4


def crypto_constants() -> dict:
    """The Argon2id parameters, read out of the Kotlin that defines them."""
    text = CRYPTO.read_text(encoding="utf-8")
    wanted = {
        "ITERATIONS": None,
        "MEMORY_KIB": None,
        "PARALLELISM": None,
        "KEY_BITS": None,
        "SALT_BYTES": None,
        "NONCE_BYTES": None,
        "NONCE_PREFIX_BYTES": None,
        "CHUNK_BYTES": None,
    }
    for name in wanted:
        for line in text.splitlines():
            if f"const val {name}" in line:
                value = line.split("=", 1)[1].strip()
                # `1 shl 20` is Kotlin for a megabyte, and reading it rather
                # than pinning the number is the point of this whole function.
                if "shl" in value:
                    left, right = value.split("shl")
                    wanted[name] = int(left.strip()) << int(right.strip())
                else:
                    wanted[name] = int(value)
                break
        if wanted[name] is None:
            raise SystemExit(f"could not read {name} from ExportCrypto.kt")
    return wanted


def row_counts(path: Path) -> dict:
    """Every user table and how many rows it holds, zeroes included.

    The manifest's job is to let an importer state plainly what is about to be
    imported, and a table silently missing from that list is indistinguishable
    from a table that was never in the format.
    """
    db = sqlite3.connect(path)
    try:
        tables = [
            name
            for (name,) in db.execute(
                "SELECT name FROM sqlite_master WHERE type = 'table' "
                "AND name NOT LIKE 'sqlite_%' ORDER BY name"
            )
        ]
        return {t: db.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0] for t in tables}
    finally:
        db.close()


def subject_count(path: Path) -> int:
    db = sqlite3.connect(path)
    try:
        return db.execute("SELECT COUNT(*) FROM subject WHERE deleted_at IS NULL").fetchone()[0]
    finally:
        db.close()


def chunk_nonce(prefix: bytes, index: int) -> bytes:
    """Frame N's nonce: the file's random prefix, then N as eight big-endian bytes.

    The same rule `ExportCrypto.chunkNonce` uses. Never random per frame: random
    96 bit nonces collide at a rate that is fine for a handful of messages and
    not fine for the millions of frames a large archive would have, and a
    collision under one key breaks GCM outright.
    """
    return prefix + index.to_bytes(8, "big")


def frame_aad(index: int, last: bool) -> bytes:
    """What a frame authenticates besides itself: where it sits, and whether it ends the file.

    Without the last byte a stream can be cut short and every remaining frame
    still verifies, so what comes out decrypts perfectly and is missing a year.
    """
    return index.to_bytes(8, "big") + (b"\x01" if last else b"\x00")


def seal(aes, prefix: bytes, plain: bytes, chunk: int) -> bytes:
    """The payload, framed and encrypted, exactly as the app writes it."""
    out = bytearray()
    frames = max(1, (len(plain) + chunk - 1) // chunk)
    for index in range(frames):
        piece = plain[index * chunk:(index + 1) * chunk]
        sealed = aes.encrypt(
            chunk_nonce(prefix, index), piece, frame_aad(index, index == frames - 1),
        )
        out += len(sealed).to_bytes(FRAME_HEADER_BYTES, "big") + sealed
    return bytes(out)


def filler(row_id: str, size: int) -> bytes:
    """Deterministic bytes for an attachment that cannot be an image."""
    seed = hashlib.sha256(row_id.encode("utf-8")).digest()
    return (seed * ((size // len(seed)) + 1))[:size]


def page_image(row_id: str) -> bytes | None:
    """A picture of a page, deterministic from the row id.

    **The attachments were filler bytes and the documents screen had never met
    an image.** This app stores photographs of paper, and a gallery of
    thumbnails is the whole point of the documents screen, per `DESIGN.md`
    11.7. A fixture whose attachments cannot be decoded meant every thumbnail
    fell back to its kind drawing, so the screen looked finished and had never
    rendered the thing it exists to render. That is D70 pointed at bytes rather
    than at rows.

    **It is a stand in and it says so.** A real attachment is a phone
    photograph of a letter, taken at an angle, in bad light. This is a clean
    gray page with lines on it, which exercises decoding, aspect ratio,
    cropping and memory, and does not exercise a photograph's noise or
    orientation metadata.

    Returns None where Pillow is not installed, and the caller falls back to
    filler bytes, so this never becomes a reason a fixture cannot be packed.
    """
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        return None

    import io
    import random

    rng = random.Random(row_id)
    width, height = 900, 1200
    image = Image.new("RGB", (width, height), (250, 248, 243))
    draw = ImageDraw.Draw(image)

    # A heading block, then lines of "text" at varying lengths, then a gap and
    # a signature line. Enough that a thumbnail reads as a page rather than as
    # a gray rectangle.
    draw.rectangle((90, 110, 90 + rng.randint(320, 520), 150), fill=(60, 70, 80))
    y = 230
    while y < height - 260:
        run = rng.randint(380, 720)
        draw.rectangle((90, y, 90 + run, y + 14), fill=(150, 158, 166))
        y += 40
        if rng.random() < 0.12:
            y += 40
    draw.rectangle((90, height - 180, 400, height - 172), fill=(90, 100, 110))

    buffer = io.BytesIO()
    image.save(buffer, format="PNG", optimize=True)
    return buffer.getvalue()


def materialize_attachments(database: Path) -> list:
    """Give every attachment row bytes that actually hash to its name.

    **The fixture generator invents attachment hashes and no bytes**, which is
    right for a database file and wrong for an export: an attachment is content
    addressed, so its file name is a claim about its content, and the importer
    checks that claim. Without this, restoring a fixture failed with "this
    export refers to an attached file that is not in it", which is the importer
    being correct.

    So the bytes are invented here and the row is corrected to match, rather
    than the other way around, which is not possible. Deterministic from the
    row id, so the same fixture packs to the same archive every time.

    **Capped, and the cap is stated.** A real fixture carries a 25 MB
    attachment to exercise the size limit. Writing 25 MB of filler into every
    persona archive would make seeding slow enough that nobody does it, so the
    bytes are truncated and the row's `byte_size` is corrected to what is
    actually there. **A persona testing the attachment size limit needs a real
    file and this is not it**, which is why the number is corrected rather than
    left lying.
    """
    cap = 64 * 1024
    db = sqlite3.connect(database)
    files = []
    try:
        rows = list(db.execute("SELECT id, byte_size FROM attachment"))
        for row_id, byte_size in rows:
            size = min(byte_size or 1, cap)
            body = page_image(row_id) or filler(row_id, size)
            digest = hashlib.sha256(body).hexdigest()
            db.execute(
                "UPDATE attachment SET sha256 = ?, byte_size = ? WHERE id = ?",
                (digest, len(body), row_id),
            )
            files.append((digest, body))
        db.commit()
    finally:
        db.close()
    return files


def pack(database: Path, target: Path, passphrase: str, exported_at: int) -> None:
    attachments = materialize_attachments(database)
    plain = database.read_bytes()

    if plain[:16] != b"SQLite format 3\x00":
        raise SystemExit(
            f"{database} does not begin with the SQLite magic, so it is not the "
            f"payload this format carries. See PortabilityTest and D61."
        )

    constants = crypto_constants()
    salt = os.urandom(constants["SALT_BYTES"])
    prefix = os.urandom(constants["NONCE_PREFIX_BYTES"])

    key = Argon2id(
        salt=salt,
        length=constants["KEY_BITS"] // 8,
        iterations=constants["ITERATIONS"],
        lanes=constants["PARALLELISM"],
        memory_cost=constants["MEMORY_KIB"],
    ).derive(passphrase.encode("utf-8"))

    aes = AESGCM(key)

    manifest = {
        "format_version": FORMAT_VERSION,
        # Named for what wrote it rather than impersonating a build. An importer
        # that ever wants to know where a strange file came from should be able
        # to read the answer.
        "app_version": "fixture",
        "platform": "fixture",
        "exported_at": exported_at,
        "origin_device": "fixture-generator",
        "encrypted": True,
        "encryption": {
            "algorithm": "AES-256-GCM",
            "kdf": "Argon2id",
            "kdf_iterations": constants["ITERATIONS"],
            "kdf_memory_kib": constants["MEMORY_KIB"],
            "kdf_parallelism": constants["PARALLELISM"],
            "salt": base64.b64encode(salt).decode("ascii"),
            "nonce_prefix": base64.b64encode(prefix).decode("ascii"),
            "chunk_bytes": constants["CHUNK_BYTES"],
        },
        "database": {
            # **Hashed as it sits inside the payload**, which is the plain
            # database, since version 3 puts the hash inside the encryption.
            "sha256": hashlib.sha256(plain).hexdigest(),
            "byte_size": len(plain),
            "schema_version": FORMAT_VERSION,
            "row_counts": row_counts(database),
        },
        "attachments": {
            "count": len(attachments),
            "total_bytes": sum(len(b) for _, b in attachments),
        },
        "subject_count": subject_count(database),
    }

    # -- the inner container, which is an ordinary zip ------------------------

    readme = (
        "WHAT THIS FILE IS\n\n"
        "This is a Health Trail archive, written by the fixture generator in\n"
        "tools/fixtures/. It holds a made up notebook, not anybody's record.\n\n"
        "The format is specified byte for byte at contract/EXPORT-FORMAT.md and a\n"
        "tool that opens it is at tools/decrypt/ in the same repository.\n"
    )
    schema = (Path(__file__).resolve().parents[2] / "contract" / "schema.sql").read_text()

    inner = io.BytesIO()
    checksums = {}
    with zipfile.ZipFile(inner, "w", zipfile.ZIP_DEFLATED) as payload:
        def put(name: str, body: bytes, method=zipfile.ZIP_DEFLATED) -> None:
            payload.writestr(name, body, method)
            checksums[name] = hashlib.sha256(body).hexdigest()

        put(README, readme.encode("ascii"))
        put(INNER_MANIFEST, json.dumps(manifest, indent=2, sort_keys=True).encode("utf-8"))
        put(DATABASE, plain, zipfile.ZIP_STORED)
        put(SCHEMA, schema.encode("utf-8"))
        for digest, body in attachments:
            put(ATTACHMENTS + digest, body, zipfile.ZIP_STORED)
        payload.writestr(
            CHECKSUMS,
            "".join(f"{h}  {n}\n" for n, h in sorted(checksums.items())).encode("ascii"),
        )

    sealed = seal(aes, prefix, inner.getvalue(), constants["CHUNK_BYTES"])

    # -- the outer layer, which says nothing about anybody --------------------

    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(MANIFEST, json.dumps(manifest_public(manifest), indent=2, sort_keys=True))
        archive.writestr(README, readme)
        archive.writestr(PAYLOAD, sealed, zipfile.ZIP_STORED)

    print(f"{target}  {target.stat().st_size:,} bytes")
    print(f"  {manifest['subject_count']} subject(s), "
          f"{sum(manifest['database']['row_counts'].values()):,} rows")
    print(f"  {len(attachments)} attachment(s)")
    print(f"  open it from More, Restore from a file")


def manifest_public(manifest: dict) -> dict:
    """The outer manifest: the header, and nothing that describes the person.

    `contract/DATA-CONTRACT.md` 8.1 lists what may sit in the clear. Row counts
    alone are a profile, so the subset is written out here explicitly rather
    than by deleting keys, which is the direction that fails safe when somebody
    adds a field to the manifest and forgets this exists.
    """
    return {
        "format_version": manifest["format_version"],
        "app_version": manifest["app_version"],
        "platform": manifest["platform"],
        "schema_version": manifest["database"]["schema_version"],
        "exported_at": manifest["exported_at"],
        "encrypted": manifest["encrypted"],
        "encryption": manifest["encryption"],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--db", required=True, type=Path, help="a plain SQLite fixture")
    parser.add_argument("--out", required=True, type=Path, help="the .htx to write")
    parser.add_argument("--passphrase", required=True)
    parser.add_argument(
        "--exported-at",
        type=int,
        # **Not zero**, which renders as December 1969 on the restore screen and
        # reads as a corrupt file rather than a fixture. A fixed millisecond
        # keeps the output deterministic, which is the property that matters.
        default=1_785_000_000_000,
        help="milliseconds. The default is fixed so the archive stays deterministic.",
    )
    args = parser.parse_args()

    if not args.db.is_file():
        raise SystemExit(f"no such file: {args.db}")
    pack(args.db, args.out, args.passphrase, args.exported_at)
    return 0


if __name__ == "__main__":
    sys.exit(main())
