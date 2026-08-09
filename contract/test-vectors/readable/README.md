# The readable copy's golden vector

**Rows in, bytes out, on every push, with no device.** `contract/DATA-CONTRACT.md` 8.5, issue #9, `DECISIONS.md` B4.

## Why it exists

B4 dropped the emulator from this project on an explicit argument:

> Data survival is proven by the export and import round trip against the golden vectors in continuous integration, which is repeatable, runs on every push, and does not depend on any one device's history.

That was true about the intent and false about the repository. **Nothing in continuous integration rendered a readable page at all.** `RegenerationTest` is an instrumented test and `DateVectorTest` reads assets, so both need the phone that B4 says should not be the proof. On any day the phone was unreachable, the strongest guarantee in the format was unchecked.

This is the half that needs no Android.

## What is here

| File | What it is |
|---|---|
| `vector.json` | The rows, and the words they are rendered with, for English and Arabic |
| `expected/en/`, `expected/ar/` | What the renderer must produce, byte for byte |

**The rows came out of an archive the app itself wrote**, so they are rows the app could write, which is the fixture rule applied to a vector. Between them they reach **all twelve rendering decisions**, and `ReadableVectorTest` fails if a decision is ever added that the vector does not reach.

**Two locales, because almost no rendering defect is visible in English.** The archive carried `lang="ar" dir="rtl"` on pages of English for months and every test passed. Holding the Arabic bytes makes that a build failure rather than something somebody has to notice.

## Regenerating, which is deliberate and never automatic

    cd android && ./gradlew :app:testDebugUnitTest -Dhealthtrail.vector.write=true

Then **read the diff before committing it.** A diff here means the archive's permanent text or its layout changed, and both are decisions somebody made rather than accidents. A test that quietly rewrites its own expectation is a test that always passes.

## What it does not prove

Said plainly, so a green run is not read as more than it is.

It does not exercise SQLCipher, the zip layout, the encryption, or `ReadableRows`, which is the one Android piece of the readable pipeline. **It cannot see a defect that lives in reading rows out of a database** rather than in turning rows into pages.

The container half is `tools/checks/check_decrypt_tool.py`, which opens a real archive with the standalone decryptor in continuous integration. The two halves meet in `RegenerationTest`, on the phone.

## One thing the vector already caught

**The money strings are inputs to the renderer rather than claims about ICU**, and the Arabic ones were read out of a real Arabic export rather than computed here. That distinction is not academic. `java.text.NumberFormat` on the JDK renders the same call with Arabic-Indic digits while Android's ICU produced Latin ones, so two readers of this contract would write different bytes for the same archive. Filed rather than papered over.
