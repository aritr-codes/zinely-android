# Releasing Zinely

Mechanics of cutting and distributing a build. **What** ships is owned by [ROADMAP.md](ROADMAP.md)
and [zinely-v1.md](zinely-v1.md); **whether** it may ship is owned by the Release Agent review in
[CLAUDE.md](../CLAUDE.md#release-review-release-agent). This document owns only the how.

---

## 1. The release key

Zinely is distributed as a side-loaded APK. Android decides whether a build may be installed *over*
an existing one by comparing signing keys — so the key is the app's identity, not a formality.

**If the key is lost, no future build can update an installed Zinely.** Every tester would have to
uninstall, and uninstalling deletes their zines, because backup/restore does not exist yet
([zinely-v1.md §7](zinely-v1.md) blocker 2). Treat the keystore as irreplaceable.

### One-time setup

Generate the keystore **outside the repository** — this command is run by the person who will own
the credential, not by an agent, so the passwords never enter a transcript or a log:

```bash
keytool -genkeypair -v \
  -keystore ~/zinely-release.jks \
  -alias zinely \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Zinely, O=Zinely, C=IN"
```

Then create `keystore.properties` at the repository root (git-ignored):

```properties
storeFile=C:/Users/<you>/zinely-release.jks
storePassword=<store password>
keyAlias=zinely
keyPassword=<key password>
```

CI reads the same four values from `ZINELY_KEYSTORE_FILE`, `ZINELY_KEYSTORE_PASSWORD`,
`ZINELY_KEY_ALIAS`, `ZINELY_KEY_PASSWORD` instead.

**Back up the `.jks` file and both passwords to somewhere that survives this machine dying**
(password manager + one offline copy). This is the single highest-consequence artifact in the
project.

### Backing it up — founder instructions

**No agent, script, or CI job can do this or verify it was done.** The passwords exist only in
`keystore.properties` on this machine; they were generated in a shell and never printed, so there is
no second copy anywhere, and nothing in the repository or the build output would reveal that the
backup is missing. The first symptom of having skipped it is a build that cannot be shipped.

**What must be backed up — two files, both untracked, both required together:**

| File | Where it is now | What it is |
|---|---|---|
| `zinely-release.jks` | `C:\Users\HP\zinely-release.jks` (the path named by `storeFile`) | The private key. Irreplaceable. |
| `keystore.properties` | the repository root | The two passwords that open it, plus the alias. **The only copy of those passwords.** |

Neither file is in git — `.gitignore` excludes them deliberately, and that is correct. It also means
`git clone` on a new machine gets you a repository that cannot produce a shippable build.

**Where to put them — at least two places that fail independently:**

1. **A password manager** (1Password, Bitwarden, KeePass). Store the two passwords as fields, and
   attach the `.jks` file itself to the same entry. Note the alias (`zinely`) with them.
2. **One offline copy** — an encrypted USB stick or an external drive kept somewhere other than
   where this laptop lives. Both files together.

Do not email them to yourself, put them in a repository (public *or* private), or leave the only
copy in a cloud drive that is signed in on this same machine. Each of those fails at the same moment
the laptop does, or leaks the key while looking like a backup.

**Verify the backup rather than assuming it.** On another machine, or after deliberately renaming
the local copies, the credentials should still produce a signed build. The cheap version of that
check, run anywhere with a JDK:

```bash
keytool -list -v -keystore <path to the backed-up .jks> -alias zinely
```

If it prints a certificate with `Owner: CN=Zinely, O=Zinely, C=IN`, the backed-up file and the
backed-up password agree. If it asks again or errors, the backup is not a backup.

**Why it is irreversible.** Android identifies an app by its signing key, not by its name. Lose the
key and no build you ever make again can install as an update over the Zinely on a tester's phone —
the only route is a new key, which means every tester uninstalls, and **uninstalling deletes their
zines**, because backup/restore does not exist. There is no recovery process, no appeal, and no
support channel that can reissue it: the key is not registered with anyone. It is a file you either
have or do not.

All four values are required together. Supplying only some of them is a configuration error and
fails the build naming the missing ones — half-configured signing used to fail deep inside AGP with
a message that named neither the missing credential nor this file.

### The fallback, and the gate on it

With no keystore configured, the release build falls back to the debug key so a fresh clone stays
buildable — but `packageRelease` then **fails**, and no APK is produced:

> `zinely: refusing to package a <version> release APK signed with the debug key.`

To get an undistributable debug-signed release build on purpose (CI smoke, perf profiling), opt out
explicitly:

```bash
./gradlew :app:assembleRelease -PallowDebugSignedRelease
```

**This gate runs at execution time, deliberately.** The first version of it was a configuration-phase
`logger.warn`, and with `org.gradle.configuration-cache=true` a cached configuration is not re-run —
so the warning silently never printed and the build produced a debug-signed APK under the release
filename with a clean log. A human reading build output is not a gate; a failing task is.

## 2. Cutting a build

1. **Version** — bump `zinelyVersionName` **and** `versionCode` in [app/build.gradle.kts](../app/build.gradle.kts).
   `versionCode` must increase on every distributed build; Android compares only that when deciding
   whether an APK is an upgrade.
2. **Changelog** — move `[Unreleased]` into a dated version section in [CHANGELOG.md](../CHANGELOG.md),
   with an honest **Known limitations** list. Overpromising wording is a Release-Agent finding.
3. **Tests** — `bash tools/grun.sh <module>:test` per module; goldens must be verified, not just recorded.
4. **Assemble** —
   ```bash
   bash tools/grun.sh :app:assembleRelease --no-daemon \
     -Porg.gradle.java.installations.auto-detect=false \
     -Porg.gradle.java.installations.paths="C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
   ```
   Output: `app/build/outputs/apk/release/zinely-<versionName>-release.apk` — the artifact name
   carries the version so testers never see an anonymous `app-release.apk`.
5. **Copy it out of `build/`, then checksum the copy** — `dist/zinely-<versionName>-release.apk`
   (git-ignored). **The APK is not byte-reproducible**: `packageRelease` can re-run and re-sign an
   unchanged source tree, producing a different file with a different SHA-256 and identical
   contents. So a published checksum identifies *one copy*, not a version — and if the copy you
   publish it for still lives under `build/`, the next build silently invalidates it. Verify the
   version and the signer from the file you are about to send, not from `output-metadata.json`:
   ```bash
   "$LOCALAPPDATA/Android/Sdk/build-tools/36.1.0/aapt2" dump badging dist/<apk> | head -1
   "$LOCALAPPDATA/Android/Sdk/build-tools/36.1.0/apksigner" verify --print-certs dist/<apk>
   ```
   `versionCode`, `versionName`, and `Signer #1 certificate DN: CN=Zinely, O=Zinely, C=IN` must all
   be what you intended. A build that verified an artifact it then rebuilt has verified nothing.
6. **Prove the artifact matches committed source** — commit the version bump and changelog first,
   then re-run the assemble: every task must report `UP-TO-DATE`. That is the cheap, honest form of
   "the APK is this commit"; a clean re-run that *executes* work means the artifact predates
   something in the tree.
7. **Verify on a clean device** — install the artifact you are about to send, on a phone that does
   not already have Zinely, and complete the full journey. A build that has only ever been verified
   as an upgrade has not been verified.
8. **Tag** — `git tag -a v<version> -m "<version> — <headline>"` on the exact commit the artifact was
   built from, and push both.

## 3. Beta distribution (side-load)

Testers install an APK directly, which Android treats as an unknown source. The tester note must say,
in plain words: what to tap to allow the install, that the app never touches the network, and — until
backup exists — **that uninstalling deletes their zines, so export anything they care about first**.

### The one-time break at 0.9.0-beta.1

Every build up to and including 0.8.0 was signed with a *debug* key. `0.9.0-beta.1` is the first
signed with the real release key, and Android will not install it over a differently-signed app —
anyone still holding `0.6.0-alpha.1` gets `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

Those testers must **export anything they want to keep, then uninstall, then install the beta**.
Their zines do not survive; there is no backup/restore yet. This was already flagged as a known
limitation of the alpha, and it is a one-time cost that ends here — every build from the beta onward
installs cleanly over its predecessor. The tester note must say so explicitly rather than let someone
discover it as an install error.

**Write the escape route in the words of the build they are holding, not the one you are shipping.**
This is the trap that nearly shipped in the `0.9.0-beta.1` package: the rescue step said
*"Print & fold → Save PDF"*, which are this build's labels. In `0.8.0` the same path reads
**Preview › → Print setup → Save PDF**, and in `0.6.0-alpha.1` **there is no save at all** — sharing
to somewhere permanent is the only export ([CHANGELOG alpha known-limitation #1](../CHANGELOG.md)).
An instruction naming a button the reader cannot find is worse than no instruction: it is the one
document standing between them and irreversible data loss, and it reads as authoritative. Check the
old tags (`git show v0.8.0:…/ProofScreen.kt`) rather than remembering.

Play Store distribution is not in use yet. It would additionally need an upload key, a Play Console
listing, a privacy policy, a content rating and a data-safety declaration.

---

## 4. Play Store — closed testing

> Added 2026-07-24 for the first Play submission. §3 above remains the record for side-load
> distribution; this section owns Play. **The build is unchanged** — the Play artifact is the same
> code as `0.9.0-beta.1`, delivered as an App Bundle instead of an APK.

### 4.1 The gate that is not code

**A new Play Console account needs identity verification, which takes 1–3 business days.** It costs
$25 once. Nothing in this repository shortens it, and it is the only step that can miss a ship date on
its own. Start it before touching anything else.

**Closed testing publishes immediately** once the account is verified and the release is reviewed.
**Production does not:** a new *personal* developer account must first run a closed test with **12
testers for 14 continuous days**. A beta wants closed testing anyway, so this is not a constraint worth
fighting — it is a reason to start the 14 days now if production is ever the goal.

### 4.2 The artifact

Play requires an **App Bundle**, not an APK:

```
./gradlew :app:bundleRelease        # -> app/build/outputs/bundle/release/zinely-<version>-release.aab
```

It is signed by the same release key as the APK (§1), read from `keystore.properties` or the
`ZINELY_KEYSTORE_*` environment variables. **Enrol in Play App Signing at upload and keep this key as
the upload key** — Play then holds the app signing key and this one only proves the upload is yours.
Back it up exactly as §1 requires; losing the upload key is recoverable, losing an un-enrolled app
signing key is not.

`versionCode` does not need bumping for the first Play upload: nothing has been uploaded to Play, so
`3` is free, and reusing it keeps the Play build honestly identified as the same build the side-load
cohort received.

### 4.3 The console checklist

| # | Item | Notes |
|---|---|---|
| 1 | **App name** | `Zinely` (30 char limit) |
| 2 | **Short description** | 80 char limit — see §4.4 |
| 3 | **Full description** | 4000 char limit — see §4.4 |
| 4 | **App icon** | 512×512 PNG, 32-bit, no alpha |
| 5 | **Feature graphic** | **1024×500** PNG or JPEG — mandatory, and the one asset with no source in this repo |
| 6 | **Phone screenshots** | 2–8, min 320px, max 3840px, 16:9 or 9:16. Shelf · Editor · Read · Print & fold is the honest four |
| 7 | **Privacy policy URL** | [docs/PRIVACY-POLICY.md](PRIVACY-POLICY.md), hosted anywhere public (GitHub Pages, a gist, any static host) |
| 8 | **Data safety form** | See §4.5 — every answer is "no" |
| 9 | **Content rating questionnaire** | No user-generated content *sharing*, no ads, no data collection |
| 10 | **Target audience** | 13+ is the safe answer; the app has no child-directed content |
| 11 | **Ads declaration** | **No ads** |
| 12 | **Government app / financial features** | No |
| 13 | **Release notes** | 500 char limit per language — see §4.4 |

### 4.4 Listing copy

**Short description** (73 / 80):

```
Make a printable zine on your phone. No account, no cloud, works offline.
```

**Full description:**

```
Zinely turns your phone into a small printing press.

Make an eight-page zine, add your photos and words, and print it on one sheet of ordinary
A4 or Letter paper. Fold it, cut one slit, and you are holding a booklet.

WHAT IT DOES

• Eight pages on one sheet. Zinely works out the imposition — which page goes where, and
  which way up — so a single-sided print folds into a booklet in the right order.
• Your photos, framed how you want. Pick photos with the Android photo picker, then move
  and zoom them inside the frame.
• Words, styled. Set size, alignment, bold, italic, and colour. What you see is what prints.
• Read your zine. Page through your own work, one page per screen, before you print it.
• Print and fold, guided. A print recipe that tells you the settings that matter, and a fold
  guide for the cut.
• Save a PDF to your phone, or share it wherever you like.

PRIVACY, PLAINLY

Zinely has no account, no cloud, and no analytics. It does not ask for internet access at all,
so your zines cannot leave your phone unless you export them yourself. Your work lives in
Zinely's own private storage and nowhere else.

BEFORE YOU START — THIS IS A BETA

• There is no backup or restore yet. Your zines live only on this phone. Uninstalling Zinely
  deletes them. Save a PDF of anything you care about.
• Print at 100% or "Actual size". A printer's "fit to page" shifts everything and breaks the
  fold alignment.
• Text renders in the bundled Inter family only. Non-Latin scripts — Bengali, Hindi, CJK — and
  emoji will not appear yet.
• Choosing a font is not in this build.
• Deleting a photo does not yet reclaim its storage.

Zinely is made for beginners. If something confuses you, that is a bug — please tell us.
```

**Release notes for `0.9.0-beta.1`** (< 500 chars):

```
The first Zinely beta.

NEW: "Read" opens your zine — your pages, one per screen, in reading order. Print & fold now
lives behind its own button.

NEW: Style your text — size, alignment, bold, italic, and five inks, live on the page.

FIXED: a reopened zine could be permanently broken by adding to it. Affected zines repair
themselves on next open.

Please note: there is no backup yet. Uninstalling deletes your zines — save a PDF first.
```

### 4.5 Data safety declaration

Every answer is the same, and each is verifiable from the manifest:

- **Does your app collect or share any of the required user data types?** — **No.**
- **Is all of the user data collected by your app encrypted in transit?** — n/a (no data leaves the
  device; the app declares no `INTERNET` permission).
- **Do you provide a way for users to request that their data is deleted?** — **Yes**, in-app deletion
  and uninstall; state that the app stores data only on-device.

The single declared permission is `WRITE_EXTERNAL_STORAGE` with `android:maxSdkVersion="28"`, needed
only to write an exported PDF into Downloads on Android 9 and older. It is not a data-collection
mechanism and does not change any answer above.

### 4.6 What is still owed before submitting

- **The feature graphic (1024×500)** — the only listing asset with no source in this repository.
- **Screenshots** from a real device on a release build.
- **A public URL for the privacy policy.**
