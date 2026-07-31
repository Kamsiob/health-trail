# web

A scaffold whose only job is to prove the schema contract is real. Empty until issue #16.

**What it is for.** It opens a database created from the same `contract/schema.sql` that the Android build reads, using SQLite compiled to WebAssembly, and reads the same template JSON from `/templates`. That single proof is what stops the two platforms drifting apart, because a schema that exists only as platform code makes the second platform a reimplementation rather than a second reader.

**What it is not.** It is not the web version of Health Trail, and it is not being built into one now. It has no features and it gains none. The progressive web app is a possible future addition listed on the roadmap.

**Two rules it will carry when it exists.** Persistent storage has to be requested explicitly, and the interface says plainly that browsers can clear local data under storage pressure, which makes backup discipline matter more on the web than on Android rather than less. And no service worker ever caches user data: the shell may be cached, the records live in the database only.
