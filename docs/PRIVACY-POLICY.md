# Zinely — Privacy Policy

**Last updated: 9 September 2026**

Zinely is an offline app for making printable zines. This policy describes what Zinely does with your
information. It is short because Zinely does very little with it.

---

## The short version

**Zinely collects nothing, uploads nothing, and has no servers.**

There is no account, no sign-in, no analytics, no advertising, no tracking, and no crash reporting.
Zinely does not request internet access, so it cannot transmit your information even accidentally.

---

## What Zinely stores, and where

Zinely keeps its working data in its own private storage on your device:

- **Your zines** — pages, text, layout and settings.
- **Photos you add to a zine** — a copy of each image you choose is kept inside the app's private
  storage so your zine still opens if the original photo is later moved or deleted.

No other app can read this private storage. Files leave it only when you explicitly save, share, or
back up your work using Android's system file interfaces.

---

## Permissions Zinely asks for, and why

| Permission | When | Why |
|---|---|---|
| **Photo picker** (no permission required) | When you add a photo | Zinely uses the Android system photo picker. You choose individual photos; Zinely never receives access to your photo library as a whole. |
| **`WRITE_EXTERNAL_STORAGE`** — *Android 9 and older only* | The first time you save a PDF | Needed to write your exported PDF into your **Downloads** folder. On Android 10 and newer, Zinely uses the system's scoped Downloads mechanism and asks for no permission at all. Zinely writes only the export file you asked for. |
| **System file picker** (no permission required) | When you make or restore a backup | You choose the backup file and its location. Zinely receives access only to the file selected through Android's picker. |
| **Vibration** (no prompt) | When you use certain controls | Provides brief touch feedback. It gives Zinely no access to personal information. |

**Zinely does not request the `INTERNET` permission.** Its legacy storage permission is limited to
Android 9 and older, and its vibration permission does not provide access to personal information.

---

## What leaves your device, and only when you ask

Nothing leaves Zinely's private storage unless you explicitly request it:

- **Save PDF** writes a PDF of your zine into your **Downloads** folder, where it becomes an ordinary
  file on your phone that you control.
- **Share** hands a PDF to Android's share sheet so you can send it somewhere yourself. What happens
  to it after that is governed by whichever app you chose — Zinely is not involved.
- **Back up** writes a `.zine` library backup to the file location you choose through Android's system
  picker. The backup stays under your control and can be used to restore your library later.
- **Restore** reads only the `.zine` backup you select and adds its valid contents to Zinely's private
  storage on the device.

Zinely never uploads or automatically syncs your work. Saving, sharing, backing up, and restoring happen
only when you ask, using destinations or files that you choose.

---

## Children

Zinely does not collect personal information from anyone, including children. There is no account, no
profile, and no communication feature.

---

## Deleting your data

Deleting a zine inside the app removes its private working copy. Uninstalling Zinely removes the app's
private library. PDFs and `.zine` backups that you previously saved outside the app remain where you put
them and must be deleted separately if you no longer want them.

> **Please note:** Zinely has no cloud backup. Before uninstalling or changing phones, create a fresh
> library backup from **Backups** and keep it somewhere you control. Without a valid backup, uninstalling
> deletes the private library and Zinely cannot recover it.

---

## Changes to this policy

If this policy changes, the "Last updated" date above changes with it, and the change will be noted in
the app's release notes.

---

## This website

The Zinely website has no analytics, advertising, contact form, or third-party fonts. It is hosted by
GitHub Pages, so GitHub may process standard technical data needed to serve the site under the
[GitHub General Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement).
This hosting does not change how the Zinely Android app handles your information.

---

## Contact

Questions about this policy or about Zinely's handling of your information:

**[aritr.g06@gmail.com](mailto:aritr.g06@gmail.com)**
