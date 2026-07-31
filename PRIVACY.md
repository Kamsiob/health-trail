# Privacy policy for Health Trail

**The canonical policy for this app is at [kamsiob.com/health-trail.html#privacy](https://kamsiob.com/health-trail.html#privacy).**

That is the URL the About screen links, the URL used in the Play Console listing, and the version that governs. This file mirrors it so the repository is not missing a policy, and it is kept identical whenever the hosted version changes. If this file and that page ever disagree, the page is correct and this file is a bug.

**A note for anyone maintaining this, because it is easy to get wrong.** The site also carries a longer, all-products policy at `privacy.html#health-trail`, and the canonical page links to it as "the full policy". That link is not a statement that the longer page governs. For Health Trail, the canonical policy is the one above, and it is the one that goes in the store listing and the app. Do not switch them. This has already been got wrong once by following the link rather than the instruction.

---

## The policy, as published

**Collected.** Nothing. Not anonymized, not aggregated. Not collected.

**Shared.** With no one. No account, no cloud, no sync to anyone's server. Ever.

**Your records live.** On your phone. Every entry, document, and chart stays on the device.

**Ads and trackers.** Zero. No ad code, no tracking code, no third-party eyes.

**Sharing.** Your call, as PDF. You generate it and send it through your own share sheet.

**Delete it all.** Uninstall. Done. Everything goes with the app. Export a backup first if you want one.

Then, in the same words:

- I never see a byte of what you record, and neither does anyone else. There is nothing in the app that could send it.
- App store install counts are aggregate numbers from the store's own systems; nothing is reported from inside Health Trail.
- Not directed at children under 13, and no data is knowingly collected from anyone, because no data is collected at all.

Made by Kamsiob. Questions to hello@kamsiob.com. Any change gets posted on the canonical page with a new date.

---

## How the software actually delivers that

This section is not part of the policy and does not add to or narrow it. It is here because this is a public repository, and someone reading the code deserves to know how the promises above are kept and where to check.

**There is no network code.** The app declares no `INTERNET` permission and has no HTTP client, no socket, and no analytics or crash reporting library. The merged manifest is audited after every dependency addition, because libraries add permissions silently. Check `android/app/src/main/AndroidManifest.xml`.

**The database is encrypted at rest** with a key generated in and held by the Android Keystore, which never leaves the device and is never written to preferences, a file, or a log.

**Platform backup is switched off deliberately.** `allowBackup` is false and `data_extraction_rules.xml` excludes every domain from both cloud backup and device transfer. Android's own backup would have copied a notebook containing another person's health information into a Google account, which would break the policy above without anyone being asked. Moving to a new phone uses the app's own export instead: created deliberately, written to a folder you chose, encrypted with a passphrase you set.

**Export encryption is separate from the at-rest encryption** and defaults on. A portable file cannot depend on one device's keystore, so it uses a passphrase you choose. If that passphrase is lost the file cannot be recovered, because there is no server, no recovery code, and no backdoor, and the app says exactly that before you commit to it. An unencrypted export is available for inspecting your own data, with a plain warning rather than a scolding.

**Deleted means deleted.** The app keeps a marker rather than a hole when you delete something, so a future device-to-device transfer cannot resurrect it. That marker is internal. Anything you delete appears in no search, no digest, no chart, no count, and no export, and this is enforced by the shape of the code rather than by remembering. The full data wipe removes everything, markers included.

**Permissions,** each requested only at the moment it is needed: the camera, when you photograph a document, and write access to a folder you pick, only if you turn on local backup.

**Health Trail is a record-keeping app, not a medical app.** It gives no medical or legal advice and never interprets what you record. You are responsible for what you write down and for who you share exports with.
