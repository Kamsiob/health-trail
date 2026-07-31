# Privacy policy for Health Trail

**The canonical version of this policy is hosted at [kamsiob.com/privacy.html#health-trail](https://kamsiob.com/privacy.html#health-trail).** That page is the one the app links to and the one that governs. This file mirrors it so the repository is not missing a policy, and it is kept identical whenever the hosted version changes.

If this file and the hosted page ever disagree, the hosted page is correct and this file is a bug.

---

## The policy, as published

> Everything you record stays on your phone. Full stop.
>
> No account, no cloud, no sync to anyone's server, ever. I never see a byte of what you record, and neither does anyone else.
>
> Sharing happens only as a PDF you generate and send through your own share sheet.
>
> App store install counts come from the store's own systems; nothing is reported from inside the app.
>
> Not directed at children under 13; no data is knowingly collected from anyone.
>
> Deleting the app deletes everything. Export a backup first if you want one.

The summary on the Health Trail page says the same thing in fewer words:

> **Collected:** Nothing. Not anonymized, not aggregated. Not collected.
>
> **Shared:** With no one. No account, no cloud, no sync to anyone's server. Ever.
>
> **Your records live:** On your phone. Every entry, document, and chart stays on the device.
>
> **Ads and trackers:** Zero. No ad code, no tracking code, no third-party eyes.
>
> **Sharing:** Your call, as PDF. You generate it and send it through your own share sheet.
>
> **Delete it all:** Uninstall. Done. Everything goes with the app. Export a backup first if you want one.

Questions go to hello@kamsiob.com. If the policy ever changes, the change lands on the hosted page first, in the same plain words, with a new date.

---

## How the software actually delivers that

This section is not part of the policy. It is here because this is a public repository and someone reading the code deserves to know how the promises above are kept, and where to look to check. Nothing here adds to or narrows the policy.

**There is no network code.** The app declares no `INTERNET` permission, has no HTTP client, no socket, and no analytics or crash reporting library. The manifest is audited after every dependency addition, because libraries add permissions silently. You can verify this by reading `android/app/src/main/AndroidManifest.xml`.

**The database is encrypted at rest** with a key generated in and held by the Android Keystore, which never leaves the device and is never written to preferences, a file, or a log.

**Platform backup is switched off deliberately.** `allowBackup` is false and `data_extraction_rules.xml` excludes every domain from both cloud backup and device transfer. Android's own backup would have copied a notebook containing another person's health information into a Google account, which would break the policy above without anyone being asked. Moving to a new phone uses the app's own export instead, which you create deliberately, write to a folder you chose, and encrypt with a passphrase you set.

**Export encryption is separate from the at-rest encryption,** and defaults on. A portable file cannot depend on one device's keystore, so it uses a passphrase you choose. If that passphrase is lost the file cannot be recovered: there is no server, no recovery code, and no backdoor, and the app says exactly that before you commit to it. An unencrypted export is available for inspecting your own data, with a plain warning rather than a scolding.

**Deleted means deleted.** The app keeps a marker rather than a hole when you delete something, so that a future device-to-device transfer cannot resurrect it. That marker is internal. Anything you delete appears in no search, no digest, no chart, no count, and no export, and this is enforced by the shape of the code rather than by remembering. The full data wipe removes everything including those markers.

**Permissions,** each requested only at the moment it is needed: the camera, when you photograph a document, and write access to a folder you pick, only if you turn on local backup.

**Health Trail is a record-keeping app, not a medical app.** It gives no medical or legal advice, and it never interprets what you record. You are responsible for what you write down and for who you share exports with.
