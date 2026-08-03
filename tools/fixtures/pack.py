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

`contract/export-format.md` is the specification. This writes format version 2,
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

FORMAT_VERSION = 2
MANIFEST = "manifest.json"
DATABASE = "data.sqlite"
ATTACHMENTS = "attachments/"


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
    }
    for name in wanted:
        for line in text.splitlines():
            if f"const val {name}" in line:
                wanted[name] = int(line.split("=")[1].strip())
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


def nonce_for(name: str, length: int) -> bytes:
    """A nonce for one attachment, from its content hash.

    The same rule `ExportContainer.nonceFor` uses: the file's name **is** its
    SHA-256, so it is unique within the archive by construction, which is
    exactly the property a nonce needs, and the first twelve bytes of the hash
    of that name give a distinct nonce per file without storing one each in the
    manifest.
    """
    return hashlib.sha256(name.encode("utf-8")).digest()[:length]


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
    nonce = os.urandom(constants["NONCE_BYTES"])

    key = Argon2id(
        salt=salt,
        length=constants["KEY_BITS"] // 8,
        iterations=constants["ITERATIONS"],
        lanes=constants["PARALLELISM"],
        memory_cost=constants["MEMORY_KIB"],
    ).derive(passphrase.encode("utf-8"))

    aes = AESGCM(key)
    stored = aes.encrypt(nonce, plain, None)

    # **Each attachment gets its own nonce, derived the way the app derives
    # it.** Reusing one nonce under one key is the mistake that breaks GCM
    # outright rather than merely weakening it.
    packed_attachments = [
        (digest, aes.encrypt(nonce_for(digest, constants["NONCE_BYTES"]), body, None))
        for digest, body in attachments
    ]

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
            "nonce": base64.b64encode(nonce).decode("ascii"),
        },
        "database": {
            # **Hashed as stored**, meaning the ciphertext, which is what the
            # importer checks before it has a key to decrypt anything with.
            "sha256": hashlib.sha256(stored).hexdigest(),
            "byte_size": len(stored),
            "schema_version": FORMAT_VERSION,
            "row_counts": row_counts(database),
        },
        "attachments": {
            "count": len(packed_attachments),
            "total_bytes": sum(len(b) for _, b in packed_attachments),
        },
        "subject_count": subject_count(database),
    }

    # The manifest is written last and stored first, per section 6 of the
    # format: its hash describes the payload, so the payload has to exist
    # before it can be true, and a reader has to be able to say what a file is
    # before it can ask for a passphrase.
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(MANIFEST, json.dumps(manifest, indent=2, sort_keys=True))
        archive.writestr(DATABASE, stored, zipfile.ZIP_STORED)
        for digest, body in packed_attachments:
            archive.writestr(ATTACHMENTS + digest, body, zipfile.ZIP_STORED)

    print(f"{target}  {target.stat().st_size:,} bytes")
    print(f"  {manifest['subject_count']} subject(s), "
          f"{sum(manifest['database']['row_counts'].values()):,} rows")
    print(f"  {len(packed_attachments)} attachment(s)")
    print(f"  open it from More, Restore from a file")


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
