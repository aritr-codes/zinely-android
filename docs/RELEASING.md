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

Those testers must **export anything they want to keep (Save PDF to Downloads), then uninstall,
then install the beta**. Their zines do not survive; there is no backup/restore yet. This was
already flagged as a known limitation of the alpha, and it is a one-time cost that ends here — every
build from the beta onward installs cleanly over its predecessor. The tester note must say so
explicitly rather than let someone discover it as an install error.

Play Store distribution is not in use yet. It would additionally need an upload key, a Play Console
listing, a privacy policy, a content rating and a data-safety declaration.
