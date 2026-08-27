# Security

Health Trail holds health information about someone's family member, on their own phone, with no account and no server. That shapes what a vulnerability means here.

## Reporting a vulnerability

Report it privately, not in a public issue.

- **Preferred:** [open a private security advisory](https://github.com/Kamsiob/health-trail/security/advisories/new) on this repository.
- **Or email:** hello@kamsiob.com

Please include what you found, how to reproduce it, and what an attacker could actually get. If you have a proof of concept, do not use real notebook data in it.

## What to expect

One person maintains this. A first reply usually comes within a few days. If a report is genuine, you will be told what the fix is and when it ships, and credited in the release notes unless you would rather not be.

If you do not hear back within two weeks, send a follow-up. It means the message went missing rather than that it was ignored.

## What counts as a vulnerability here

The app makes a small number of specific promises, and anything that breaks one of them is in scope:

- **Data leaving the device.** The app makes no network calls. Anything that causes it to, including through a dependency, is a serious bug.
- **Data readable outside the app.** The database is encrypted at rest with a key held in the Android Keystore. Anything that exposes the plaintext database, the key, or the passphrase for an export is in scope.
- **Deleted data that is not deleted.** The app uses tombstones internally so a future sync can work, but something the user deleted must not appear in any search, digest, chart, or export. A path where it does is a privacy bug, not a display bug.
- **Export encryption weaknesses.** An export is a file that lands in a folder the user chose and may be synced elsewhere by something they already run. Weak or bypassable encryption on it matters.
- **An import that can harm the app or the device.** A malicious export file must fail cleanly and change nothing.
- **Anything that can read another app's data, or let another app read this one's.**

## What is not a vulnerability

- **Physical access to an unlocked phone.** The app does not defend against someone holding the unlocked device. It relies on the device lock.
- **The user choosing an unencrypted export.** That is offered deliberately, with a plain warning, for people who want to inspect their own data.
- **A lost export passphrase.** There is no recovery, no server, and no backdoor. That is the design, and the app says so before the user commits to it.
- **Missing certificate pinning, missing network security configuration, and similar.** The app makes no network calls, so these have nothing to apply to.

## Supported versions

The most recent release on Google Play is the only supported version, and the GitHub release asset is the same build. Fixes ship forward rather than being backported.
