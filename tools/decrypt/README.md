# Opening a Health Trail archive without Health Trail

If you have a Health Trail archive and the passphrase, this folder gets your
record back. You do not need the app, the phone it came from, or an internet
connection.

**You do not need to understand any of this to use it.** Follow the four steps.

---

## What you have

A file with a name like `health-trail-2027-03-14.zip`. Inside it is somebody's
care notebook: their notes, the calls they made, the questions they asked, and
photographs of the paperwork. It is encrypted, so opening it with an ordinary
zip program shows you scrambled files. That is expected.

**You need the passphrase.** There is no way around it and nobody can reset it.
There is no server, no recovery code, and no backdoor. If the passphrase is
gone, the archive cannot be opened by anyone, including whoever wrote this.

---

## Step 1. Get Python

Most Macs and Linux computers already have it. Open a terminal and type:

```
python3 --version
```

If that prints a number of 3.9 or higher, you have it. If it says the command
was not found, install Python from https://www.python.org/downloads/ and try
again.

## Step 2. Get the two libraries

In the same terminal:

```
pip install cryptography argon2-cffi
```

This downloads two standard, widely used pieces of software that do the actual
unscrambling. It needs the internet once. After that, nothing here does.

## Step 3. Run it

Put your archive and this folder somewhere you can find them, then:

```
python3 decrypt.py health-trail-2027-03-14.zip my-record
```

Replace the file name with yours. `my-record` is the folder it will create; you
can call it anything.

It will tell you what the archive contains, ask for the passphrase, and then
take a few seconds. **The slowness is on purpose**: it is what makes the
passphrase hard to guess.

## Step 4. Read it

Open the folder it made. Inside is `readable`, and inside that is
`index.html`. Double click it, or open it in any web browser.

That is the record: every entry, every person, every document, laid out as
ordinary pages. **It needs no software and no internet.** You can print it, mail
it, or keep it on a memory stick for twenty years.

---

## What else is in there

| | |
|---|---|
| `readable/` | The record as web pages. **Start here.** |
| `attachments/` | The original photographs and documents, as ordinary image and PDF files. |
| `data.sqlite` | The same record as a database file, for giving back to the app or reading with any SQLite tool. |
| `manifest.json` | A short technical header describing the archive. |

Every entry in the readable pages shows a reference code, and that is the same
code the database uses, so anything you find in one you can look up in the
other.

---

## If it does not work

**"Could not decrypt the record."** The passphrase is not the one the archive was
made with, or the file was damaged or altered after it was made. **The tool
cannot tell which**, and it does not guess: the check that fails cannot
distinguish a wrong passphrase from an altered file. If you are sure of the
passphrase, try another copy of the archive.

**"This archive says it is format version N."** The archive was written by a
newer version of the app than this tool knows about.
`contract/EXPORT-FORMAT.md` in the Health Trail repository specifies every
version byte for byte, so a newer tool can be written from it.

**"This needs the 'cryptography' library."** Step 2 did not finish. Run it again
and read what it says.

---

## For anyone who needs to write their own

`decrypt.py` is about two hundred lines and every step in it is described in
`contract/EXPORT-FORMAT.md`. It was written from that document rather than from
the app's source code, which is deliberate: **if the specification is enough to
build the tool, it is enough for somebody else to build one too**, years from
now, in whatever language exists then.

Everything here is AGPL licensed, like the rest of Health Trail. You may copy
it, change it, and pass it on.
