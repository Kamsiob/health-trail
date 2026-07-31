# android

The Kotlin application. Empty until issue #11.

**What goes here.** A single activity Jetpack Compose application, Material 3 with a fully custom theme implementing the `DESIGN.md` tokens, and one SQLite database encrypted at rest with SQLCipher using a key held in the Android Keystore.

**What does not go here.** The schema. It lives in `contract/schema.sql` and the build copies it into assets, failing loudly rather than falling back to a stale internal copy. No Kotlin source file in this directory declares a table or a column, and a check in continuous integration enforces that.

The same applies to the export format, the message catalogs, and the template data. This directory reads from `/contract` and `/templates`. It never keeps a second copy of anything either of them owns.
